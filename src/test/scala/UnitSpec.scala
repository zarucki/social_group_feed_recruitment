import org.scalatest.{Assertions, FlatSpec, Inspectors, OptionValues}

abstract class UnitSpec extends FlatSpec with Assertions with OptionValues with Inspectors {}