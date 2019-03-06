# Social group feed - recruitment task

## Task requirements

>You're required to write a backend + REST APIs for a simple posting in groups. The application will allow users to post into groups and read post feeds
>* let's assume all groups are "open", so any user can join any group as they wish
>* user must be a member of the group in order to post into it or read it's feeds
>* every group needs to have a feed, that is essentially a list of posts sorted from newest to oldest
>* there is also an "All groups feed" that displays feed of all posts for all groups user is a member of, sorted from newest to oldest
>* for simplicity, we'll assume that posts can't be edited or deleted once it's posted.
>* for the simplicity, users cannot leave group once becoming a member
>* for the simplicity, we'll assume text-only posts, no images, no attachments, no text formatting
>* for simplicity, there will be no comments
>* we will assume groups are already created, any valid integer is assumed to be a valid group ID of an existing group
>* post data in the feed must contain: ID, datetime created, content, author's user ID, author's name
>* post data in "All groups feed" must also contain group ID
>* keep in mind performance of "All groups feed" when the database will be really large

Required APIs:
>* get list of groups user is member of (in our simplified version it'll be just the list of IDs)
>* become member of a group
>* add post to a group
>* get post feed of a group
>* get "All groups feed" (post feed of all groups user is member of)

Constraints:
>* let's assume User to be defined as Name and ID (no avatar or profile)
>* let's assume highly simplified authentication (eg. user ID serves as auth token) or even no authentication
>* choose whatever stack you want and whatever persistance you want, the only requirement is that you use Scala and Akka (in any way you see fit)
>* application should be easy to run on localhost via `sbt run` or similar. If there are some external requirements that need to be installed
>* prior to running it (eg. some database, etc), please add a ReadMe file with a necessary description
>* deliver your solution on any git-based system where we can access it (GitHub, GitLab, etc.). Might even be private, as long as we get read permissions to it
>* preferably, task should be done as a normal project, with each step being identified as a commit instead of a single commit for the whole thing

>Above are minimum requirements. You're welcome to come up with your own improvements if you wish so, but only as long as complexity is the same or higher than above.

# Solution

## Running

The http server needs to have mongodb running, there is `docker-compose.yml` file which starts two services `mongo` and `mongo express`. `mongo express` is simple management tool to view and manipulate collections.

To use `docker-compose.yml` you need to have docker and docker-compose installed. I provided install script for docker-compose which you can run:

    $ sudo ./setup/install-docker-compose.sh

When docker and docker-compose will be ready you can start mongo instance by:

    $ docker-compose up -d

To stop it you can run:

    $ docker-compose stop

To get get rid of any existing data and get clean mongo you can delete volumes by running:

    $ docker-compose down -v

The app is http-akka rest server, which you can start by:

    $ sbt run

It listens by default on `localhost:8080`.

With running rest server and mongo you can try some curl scripts in `curls` directory. For example there is one longer chain of operations when you run script:

    $ ./curls/moreComplexUserScenario.sh

There you can also see how curls for every endpoint should look like more or less.

# Rest endpoints

* GET
  * user/{userId}/groups
  * user/{userId}/all-groups-feed
  * group/{groupId}/feed
* POST
  * user/{user-id}/add-to-group/{group-id} // without any body
  * group/{group-id} // with body {content: String, userId: String, userName: String}

# Trade offs and design choices

Taking into consideration that I needed to design for really large data, I decided to use MongoDB as persistance layer. Also I knew you're using it to hold your data, so that's additional plus.

To achive scalability and performance I heavily used mongo indexes. Collections and indexes are designed in such way that we could later shard them among many nodes (not enough time to do that).
I had no time to benchmark properly, but I did inspect executions plans to make sure all indexes are in place and we don't hit scan.

The architecture itself is inspired by mongo lab project https://github.com/mongodb-labs/socialite .

Main points are following:
* We want to latest posts in the feed to be as fast as possible to keep users engaged
* That's why we want to trade off storage for speed by using cache for user feeds
* But we don't want to pay storage for users that are not active or dead
* If such user will come after some time, we will rebuild his cache and he will again have nice experience
* When someone posts we see who is subscribed to the group and we update caches of those people
* Timeline cache expires after time to live value, so after user inactivity the caches clears by itself
* New post is propagated only to live timeline caches, so it does not have to be always hundred of people caches to update
* There is no point in storing too much data in timeline cache, content gets old fast in social media, we only need to store first page

# Mongo collections

* `user_groups(userId: String, groupId: String)`
  * `index (userId: ascending, groupId: ascending) unique = true`
  * compound key here so we dont have to look at document at all, we have groupId we are looking for right there in the index
  * `userId` could be a shard key - index prefixes work with sharding
* `group_user_members(groupId: String, userId: String)`
  * `index (groupId: ascending, userId: ascending) unique = true`
  * same as above, but different direction
  * this redundant collection is used instead of creating second index on user\_groups because we want to have working shard key in other direction as well
  * `groupId` could be a shard key - index prefixes work with sharding
* `post_ownerships(ownerId: String, postId: ObjectId)`
  * `index (ownerId: ascending, postId: descending) unique = true`
  * descending because we want to have latest posts first
  * because of order of fields in the index we need to carefully page results, we could get results from some popular group with lot of posts and not see that some later small group has some fresh post
  * here we save relation between user-\>post and group-\>post
  * `ownerId` could be a shard key - index prefixes work with sharding
* `posts(_id, insertedAt: Timestamp, content: String, userId: String, groupId: String, userName: String)`
  * no additional index, apart from the default one on `_id`. Other ids are here just for convenience, to lookup additional information
  * we don't store createdAt because we have it inside `_id`, though we loose milliseconds, but it is a tradeoff we can make
* `timeline_cache(ownerId: String, lastUpdated: Timestamp, topPosts: Array[ObjectId])`
  * `index(ownerId: ascending) unique true` - for lookup
  * `index(topPosts: ascending)` - for sorting 
  * `index(lastUpdated: ascending) time to live - for automatically clearing`

Timeline cache could be implemented in any other store like redis or memcached, but mongo does offer interesting possibilities.

We update multiple `timeline_cache` documents each time new post is posted. It looks something like this:

    update(
         query: { ownerId: { $in: [ "userId_1", "userId_2", "userId_3" ] } },
	 update: {
		 $set: { lastUpdated: new Date(1551833235183) },
		 $push: {
		 	tp: {
				$each: [ newPostObjectId ],
		 		$slice: 50,
	 			$sort: -1
			}
		}
	},
	multi: true,
	upsert: false 
    )

* We only touch caches of users that are subscribed to given group
* We set `upsert` to false, because we want to update only `timeline_caches` that exist.
* `slice` operator is very helpful, not only it will put our new post in right place in the array, but also it will truncate end if it will exceed given amount. Thanks to that we don't have to worry that our cache growing too much, or that it will exceed 16mb per document. We have predicatable timeline cache size.
* Above together with TTL makes it that we don't have to track which users are active, if they are active they will have a cache, and will have fast first feed page
* We don't really need much more that first feed page, if someone goes deep we can handle them manually, user has more patience when digging in past


Unfortuntately I didn't have enough time to write benchmark to measure how this architecture would hold against some bigger load.

# Lacked time to do

* some benchmarking, to measure performance - some traffic generator with mix of reading and writing
* use docker-compose to set up small mongo cluster with replication and sharding
* authentication of REST using JWT
* proper REST endpoints for paging
* better cleaning up ;)

# Further improvements to be made

* When propagating new group post among all followers we don't have to update all cached timelines at once. We could do this lazily. World will not end if some users see a post 1 minute later.
* If group would be really active we don't have to propagate posts to timeline caches one by one. We could make them more lazy and batch them.
* The `timeline_cache` collection should use in-memory storage, we don't need to persist it, we can loose it all. Also journaling should be off. We need is as fast as possible. If we screw something up we can rebuild the cache. Probably such collection would need to use different storage engine, maybe even be on differently setup mongo node.
* Don't throw out whole cache if membership changes - in some cases it doesn't affect feed at all
