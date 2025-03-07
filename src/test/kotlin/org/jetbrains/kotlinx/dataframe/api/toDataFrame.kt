package org.jetbrains.kotlinx.dataframe.api

import io.kotest.matchers.shouldBe
import org.jetbrains.kotlinx.dataframe.columns.ColumnKind
import org.jetbrains.kotlinx.dataframe.kind
import org.jetbrains.kotlinx.dataframe.ncol
import org.jetbrains.kotlinx.dataframe.nrow
import org.junit.Test
import kotlin.reflect.typeOf

class CreateDataFrameTests {

    @Test
    fun `visibility test`() {
        class Data {
            private val a = 1
            protected val b = 2
            internal val c = 3
            public val d = 4
        }

        listOf(Data()).toDataFrame() shouldBe dataFrameOf("d")(4)
    }

    @Test
    fun `exception test`() {
        class Data {
            val a: Int get() = error("Error")
            val b = 1
        }

        val df = listOf(Data()).toDataFrame()
        df.ncol shouldBe 2
        df.nrow shouldBe 1
        df.columnTypes() shouldBe listOf(typeOf<IllegalStateException>(), typeOf<Int>())
        (df["a"][0] is IllegalStateException) shouldBe true
        df["b"][0] shouldBe 1
    }

    @Test
    fun `create frame column`() {
        val df = dataFrameOf("a")(1)
        val res = listOf(1, 2).toDataFrame {
            "a" from { it }
            "b" from { df }
            "c" from { df[0] }
            "d" from { if (it == 1) it else null }
            "e" from { if (true) it else null }
        }
        res["a"].kind shouldBe ColumnKind.Value
        res["a"].type() shouldBe typeOf<Int>()
        res["b"].kind shouldBe ColumnKind.Frame
        res["c"].kind shouldBe ColumnKind.Group
        res["d"].type() shouldBe typeOf<Int?>()
        res["e"].type() shouldBe typeOf<Int>()
    }

    @Test
    fun `preserve fields order`() {
        class B(val x: Int, val c: String, d: Double) {
            val b: Int = x
            val a: Double = d
        }

        listOf(B(1, "a", 2.0)).toDataFrame().columnNames() shouldBe listOf("x", "c", "a", "b")
    }
}
