package mongo.indexes
import java.util.concurrent.{TimeUnit => JTimeUnit}

case class TTLIndexSettings(expireAfter: Long, timeUnit: JTimeUnit)
