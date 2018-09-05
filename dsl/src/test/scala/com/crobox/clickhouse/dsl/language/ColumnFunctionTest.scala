package com.crobox.clickhouse.dsl.language

import com.crobox.clickhouse.TestSchemaClickhouseQuerySpec
import com.crobox.clickhouse.dsl.TableColumn.AnyTableColumn
import com.crobox.clickhouse.dsl._
import com.crobox.clickhouse.dsl.column._
import com.crobox.clickhouse.dsl.execution.ClickhouseQueryExecutor
import com.crobox.clickhouse.dsl.language.TokenizerModule.Database
import com.crobox.clickhouse.testkit.ClickhouseClientSpec
import org.joda.time._
import org.joda.time.format.DateTimeFormat
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.Future

class ColumnFunctionTest extends ClickhouseClientSpec with TestSchemaClickhouseQuerySpec with ScalaFutures with ClickhouseTokenizerModule {

  implicit val clickhouseClient = clickClient
  implicit val db: Database = clickhouseClient.database

  val ex = ClickhouseQueryExecutor.default(clickhouseClient)

  "ColumnFunction tokenisation and execution" should "succeed for ArithmeticFunctions" in {
    r(plus(3,3)) shouldBe "6"
    r(minus(3,2)) shouldBe "1"
    r(multiply(3,5)) shouldBe "15"
    r(divide(3,2)) shouldBe "1.5"
    r(intDiv(5,3)) shouldBe "1"
    r(intDivOrZero(5,3)) shouldBe "1"
    r(modulo(13,4)) shouldBe "1"
    r(gcd(3,2)) shouldBe "1"
    r(lcm(3,2)) shouldBe "6"
    r(negate(3)) shouldBe "-3"
    r(abs(-3)) shouldBe "3"
  }

  it should "succeed for ArrayFunctions" in {
    Seq(
      r(emptyArrayUInt8),
      r(emptyArrayUInt16),
      r(emptyArrayUInt32),
      r(emptyArrayUInt64),
      r(emptyArrayInt8),
      r(emptyArrayInt16),
      r(emptyArrayInt32),
      r(emptyArrayInt64),
      r(emptyArrayFloat32),
      r(emptyArrayFloat64),
      r(emptyArrayDate),
      r(emptyArrayDateTime),
      r(emptyArrayString)
    ).foreach(result => result shouldBe "[]")

    r(emptyArrayToSingle(emptyArrayUInt8)) shouldBe "[0]"
    r(range(3)) shouldBe "[0,1,2]"

    r(arrayConcat(range(2),Seq(2L,3L))) shouldBe "[0,1,2,3]"

    val arbArray = range(3)

    r(indexOf(arbArray,2L)) shouldBe "3"
    r(countEqual(arbArray,2L)) shouldBe "1"
    r(arrayEnumerate(arbArray)) shouldBe "[1,2,3]"
    r(arrayEnumerateUniq(arbArray)) shouldBe "[1,1,1]"
    r(arrayPopBack(arbArray)) shouldBe "[0,1]"
    r(arrayPopFront(arbArray)) shouldBe "[1,2]"
    r(arrayPushBack(arbArray,4L)) shouldBe "[0,1,2,4]"
    r(arrayPushFront(arbArray,4L)) shouldBe "[4,0,1,2]"
    r(arraySlice(arbArray,2,1)) shouldBe "[1]"
    r(arrayUniq(arbArray)) shouldBe "3"
    r(arrayJoin(arbArray)) shouldBe "0\n1\n2"
  }

  it should "succeed for BitFunctions" in {
    r(bitAnd(0,1)) shouldBe "0"
    r(bitOr(0,1)) shouldBe "1"
    r(bitXor(0,1)) shouldBe "1"
    r(bitNot(64)) shouldBe (255 - 64).toString
    r(bitShiftLeft(2,2)) shouldBe "8"
    r(bitShiftRight(8,1)) shouldBe "4"

  }

  it should "succeed for ComparisonFunctions" in {
    val someNum = const(10L)

    r(someNum <> 3) shouldBe "1"
    r(someNum > 3) shouldBe "1"
    r(someNum < 3) shouldBe "0"
    r(someNum >= 3) shouldBe "1"
    r(someNum <= 3) shouldBe "0"
    r(someNum isEq 3) shouldBe "0"
    r(notEquals(1,2)) shouldBe "1"
    r(_equals(2L,2)) shouldBe "1"
    r(less(1.0,200)) shouldBe "1"
    r(greater(1L,2L)) shouldBe "0"
    r(lessOrEquals(1,2)) shouldBe "1"
    r(greaterOrEquals(1,2)) shouldBe "0"
  }

  implicit class DDTStringify(ddt: DateTime) {
    def printAsDate: String = DateTimeFormat.forPattern("yyyy-MM-dd").print(ddt)

    def printAsDateTime: String = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss").print(ddt)

    def printAsYYYYMM: String = DateTimeFormat.forPattern("yyyyMM").print(ddt)

    def toStartOfQuarter = {
      val remainder = (ddt.getMonthOfYear - 1) % 3

      ddt.withDayOfMonth(1).minusMonths(remainder)
    }

    def toStartOfMin(min: Int) = {
      val remainder = ddt.getMinuteOfHour % min

      ddt
        .withSecondOfMinute(0)
        .withMillisOfSecond(0)
        .minusMinutes(remainder)
    }

    def toStartOfHr = {
      ddt
        .withMinuteOfHour(0)
        .withSecondOfMinute(0)
        .withMillisOfSecond(0)
    }
  }

  it should "succeed for DateTimeFunctions" in {
    val now = new DateTime().withZone(DateTimeZone.UTC)
    val epoch = new DateTime(0).withZone(DateTimeZone.UTC)

    def dynNow = new DateTime().withZone(DateTimeZone.UTC)

    r(toYear(now)) shouldBe now.getYear.toString
    r(toYYYYMM(now)) shouldBe now.printAsYYYYMM
    r(toMonth(now)) shouldBe now.getMonthOfYear.toString
    r(toDayOfMonth(now)) shouldBe now.getDayOfMonth.toString
    r(toDayOfWeek(now)) shouldBe now.getDayOfWeek.toString
    r(toHour(now)) shouldBe now.getHourOfDay.toString
    r(toMinute(now)) shouldBe now.getMinuteOfHour.toString
    r(toSecond(now)) shouldBe now.getSecondOfMinute.toString
    r(toMonday(now)) shouldBe now.withDayOfWeek(1).printAsDate
    r(addSeconds(now,2)) shouldBe now.plusSeconds(2).printAsDateTime
    r(addMinutes(now,2)) shouldBe now.plusMinutes(2).printAsDateTime
    r(addHours(now,2)) shouldBe now.plusHours(2).printAsDateTime
    r(addDays(now,2)) shouldBe now.plusDays(2).printAsDateTime
    r(addWeeks(now,2)) shouldBe now.plusWeeks(2).printAsDateTime
    r(addMonths(now,2)) shouldBe now.plusMonths(2).printAsDateTime
    r(addYears(now,2)) shouldBe now.plusYears(2).printAsDateTime
    r(toStartOfMonth(now)) shouldBe now.withDayOfMonth(1).printAsDate
    r(toStartOfQuarter(now)) shouldBe now.toStartOfQuarter.printAsDate
    r(toStartOfYear(now)) shouldBe now.withDayOfYear(1).printAsDate
    r(toStartOfMinute(now)) shouldBe now.toStartOfMin(1).printAsDateTime
    r(toStartOfFiveMinute(now)) shouldBe now.toStartOfMin(5).printAsDateTime
    r(toStartOfFifteenMinutes(now)) shouldBe now.toStartOfMin(15).printAsDateTime
    r(toStartOfHour(now)) shouldBe now.toStartOfHr.printAsDateTime
    r(toStartOfDay(now)) shouldBe now.withTimeAtStartOfDay().printAsDateTime
    r(toTime(now)).substring(11) shouldBe now.printAsDateTime.substring(11)
    r(toRelativeYearNum(now)) shouldBe now.getYear.toString
    r(toRelativeMonthNum(now)) shouldBe ((now.getYear * 12) + now.getMonthOfYear).toString
    r(toRelativeWeekNum(now)) shouldBe (Weeks.weeksBetween(epoch, now).getWeeks + 1).toString
    r(toRelativeDayNum(now)) shouldBe Days.daysBetween(epoch, now).getDays.toString
    r(toRelativeHourNum(now)) shouldBe Hours.hoursBetween(epoch, now).getHours.toString
    r(toRelativeMinuteNum(now)) shouldBe Minutes.minutesBetween(epoch, now).getMinutes.toString
    r(toRelativeSecondNum(now)) shouldBe Seconds.secondsBetween(epoch, now).getSeconds.toString
    r(chNow()) should (equal(dynNow.printAsDateTime) or equal(dynNow.minusSeconds(1).printAsDateTime))
    r(chYesterday()) shouldBe dynNow.minusDays(1).printAsDate
    r(chToday()) shouldBe dynNow.withTimeAtStartOfDay().printAsDate
    r(timeSlot(now)) shouldBe now.toStartOfMin(30).printAsDateTime
    r(timeSlots(now,toUInt32(1800))) shouldBe s"['${now.toStartOfMin(30).printAsDateTime}','${now.plusMinutes(30).toStartOfMin(30).printAsDateTime}']"
  }

  it should "succeed for DictionaryFunctions" in {
    //TODO Cant test without setting up a dictionary
  }
  it should "succeed for EncodingFunctions" in {
    r(hex(12)) shouldBe "0C"
    r(unhex("0C")) shouldBe "12"

    //TODO: Eek, something with bytes
//    val someUUID = UUID.randomUUID()
//    r(uUIDNumToString(uUIDStringToNum(someUUID))) shouldBe someUUID
//    r(uUIDNumToString("3"))
//    r(bitmaskToList("255.255.255.0"))
//    r(bitmaskToArray("3"))
  }
  it should "succeed for HashFunctions" in {
    val someStringData = "fooBarBaz"

    //TODO these also return the byte format
    r(halfMD5(someStringData)) shouldBe "14009637059544572277"
    r(mD5(someStringData)) shouldBe ""
    r(sipHash64(someStringData)) shouldBe ""
    r(sipHash128(someStringData)) shouldBe ""
    r(cityHash64(someStringData)) shouldBe ""
    r(intHash32(1234)) shouldBe ""
    r(intHash64(1234)) shouldBe ""
    r(sHA1(someStringData)) shouldBe ""
    r(sHA224(someStringData)) shouldBe ""
    r(sHA256(someStringData)) shouldBe ""

    r(uRLHash("http://www.google.nl/search",1))
  }
  it should "succeed for HigherOrderFunctions" in {
    val arr1 = Seq(1L,2L,3L)

    r(arrayMap[Long,Long](_ * 2L,arr1)) shouldBe "[2,4,6]"
    r(arrayFilter[Long](_ <> 2L,arr1)) shouldBe "[1,3]"
    r(arrayExists[Long](_.isEq(2L),arr1)) shouldBe "1"
    r(arrayAll[Long](_ <= 3,arr1)) shouldBe "1"
    r(arrayAll[Long](_.isEq(2L),arr1)) shouldBe "0"
    r(arrayFirst[Long](modulo(_,2L).isEq(0),arr1)) shouldBe "2"
    r(arrayFirstIndex[Long](modulo(_,2L).isEq(0),arr1)) shouldBe "2"
    r(arraySum[Long,Long](Some(_ * 2L),arr1)) shouldBe "12"
    r(arrayCount[Long](Some(_.isEq(2L)),arr1)) shouldBe "1"
    r(arrayCumSum[Long,Long](Some(_ * 2L),arr1)) shouldBe "[2,6,12]"
    r(arraySort[Long,Double](Some(_ % 3.0),arr1)) shouldBe "[3,1,2]"
    r(arrayReverseSort[Long,Int](Some(_ % 3),arr1)) shouldBe "[2,1,3]"
  }

  it should "succeed for IPFunctions" in {
    val num = toUInt32(1)
    r(iPv4NumToString(num)) shouldBe "0.0.0.1"
    r(iPv4StringToNum("0.0.0.1")) shouldBe "1"
    r(iPv4NumToStringClassC(num)) shouldBe "0.0.0.xxx"
    r(iPv6NumToString(toFixedString("0",16))) shouldBe "3000::"
    r(iPv6StringToNum("3000::")) shouldBe "0"
  }
  
  it should "succeed for JsonFunctions" in {
    val someJson = """{"foo":"bar", "baz":123, "boz":3.1415, "bool":true}"""

    r(visitParamHas(someJson,"foo")) shouldBe "1"
    r(visitParamExtractUInt(someJson,"baz")) shouldBe "123"
    r(visitParamExtractInt(someJson,"baz")) shouldBe "123"
    r(visitParamExtractFloat(someJson,"boz")) shouldBe "3.1415"
    r(visitParamExtractBool(someJson,"bool")) shouldBe "1"
    r(visitParamExtractRaw(someJson,"foo")) shouldBe "\"bar\""
    r(visitParamExtractString(someJson,"foo")) shouldBe "bar"
  }
  it should "succeed for LogicalFunctions" in {
    r(true and true) shouldBe "1"
    r(true and false) shouldBe "0"
    r(true or false) shouldBe "1"
    r(false or false) shouldBe "0"
    r(true xor true) shouldBe "0"
  }
  it should "succeed for MathFunctions" in {
    r(e()) should startWith("2.718281828")
    r(pi()) should startWith("3.14159")
    r(exp(123)) should startWith("2.619")
    r(log(123)) should startWith("4.812184")
    r(exp2(123)) should startWith("1.063382396627")
    r(log2(123)) should startWith("6.9425145")
    r(exp10(123)) should startWith("1e123")
    r(log10(123)) should startWith("2.0899")
    r(sqrt(123)) should startWith("11.090")
    r(cbrt(123)) should startWith("4.9731")
    r(erf(123)) shouldBe("1")
    r(erfc(123)) shouldBe("0")
    r(lgamma(123)) should startWith("467.41")
    r(tgamma(123)) should startWith("9.8750")
    r(sin(123)) should startWith("-0.45990")
    r(cos(123)) should startWith("-0.88796")
    r(tan(123)) should startWith("0.51792747")
    r(asin(1)) should startWith("1.5707")
    r(acos(1)) shouldBe("0")
    r(atan(1)) should startWith("0.78539")
    r(pow(123,2)) shouldBe("15129")
  }
  it should "succeed for MiscFunctions" in {}
  it should "succeed for RandomFunctions" in {}
  it should "succeed for RoundingFunctions" in {}
  it should "succeed for SplitMergeFunctions" in {}
  it should "succeed for StringFunctions" in {}
  it should "succeed for StringSearchFunctions" in {}
  it should "succeed for TypeCastFunctions" in {}
  it should "succeed for URLFunctions" in {}

  private def r(query: AnyTableColumn): String = {
    runSql(select(query)).futureValue.trim
  }

  private def runSql(query: OperationalQuery): Future[String] = {
    clickhouseClient.query(toSql(query.internalQuery,None))
  }
}
