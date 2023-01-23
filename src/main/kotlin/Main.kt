import kotlin.math.sqrt

object Calculator {
 private val tokens = ArrayList<TokenInfo>()
 private var pos = 0

 private enum class Token {
  NUMBER, OPEN, CLOSE, PRODUCT, ATOM, UNIT, SQRT
 }

 private data class TokenInfo(
  val type: Token,
  val value: String,
 )

 class Skip(msg: String) : Exception(msg)

 private val tokenList: Array<Pair<Regex, Token>> = arrayOf(
  Pair(Regex("\\d+"), Token.NUMBER),
  Pair(Regex("[+-]"), Token.PRODUCT),
  Pair(Regex("[*/%]"), Token.ATOM),
  Pair(Regex("\\^"), Token.UNIT),
  Pair(Regex("\\("), Token.OPEN),
  Pair(Regex("\\)"), Token.CLOSE),
  Pair(Regex("sqrt"), Token.SQRT),
 )

 private fun tokenize(expression: String) {
  fun space() {
   Regex("\\s+").matchAt(expression, this.pos)?.let {
    this.pos += it.value.length
   }
  }

  while (this.pos < expression.length) {
   space()
   val pos = this.pos
   for ((regex, token) in this.tokenList) {
    regex.matchAt(expression, this.pos)?.let {
     this.tokens.add(TokenInfo(token, it.value))
     this.pos += it.value.length
    } ?: continue
    break
   }
   if (pos == this.pos) throw Skip(
    "Undefined token found <" +
            Regex("\\S+").matchAt(expression, this.pos)!!.value + ">"
   )
  }
  this.pos = 0
 }

 private fun mark() = this.pos
 private fun mark(value: Int) {
  this.pos = value
 }

 private fun expect(token: Token): TokenInfo? {
  if (this.pos >= this.tokens.size) return null
  val tokenInfo = this.tokens[this.pos]
  if (tokenInfo.type != token) return null
  this.pos += 1
  return tokenInfo
 }

 private sealed class AST {
  data class Number(val value: Int) : AST() {
   constructor(token: TokenInfo) : this(token.value.toInt())
  }

  data class Add(
   val x: AST,
   val y: AST
  ) : AST()

  data class Sub(
   val x: AST,
   val y: AST
  ) : AST()

  data class Mul(
   val x: AST,
   val y: AST
  ) : AST()

  data class Div(
   val x: AST,
   val y: AST
  ) : AST()

  data class Mod(
   val x: AST,
   val y: AST
  ) : AST()

  data class Pow(
   val x: AST,
   val y: AST
  ) : AST()

  data class Sqrt(
   val x: AST
  ) : AST()
 }

 private fun product(): AST? {
  fun _0(): AST? {
   val x = atom() ?: return null
   val op = expect(Token.PRODUCT) ?: return null
   val y = product() ?: throw Skip("Expected expression after \"${op.value}\"")
   return when (op.value) {
    "+" -> AST.Add(x, y)
    "-" -> AST.Sub(x, y)
    else -> throw Skip("Unexpected Behaviour")
   }
  }

  val mark = this.mark()
  _0()?.let { return it }
  this.mark(mark)
  atom()?.let { return it }
  this.mark(mark)
  return null
 }

 private fun atom(): AST? {
  fun _0(): AST? {
   val x = unit() ?: return null
   val op = expect(Token.ATOM) ?: return null
   val y = atom() ?: throw Skip("Expected expression after \"${op.value}\"")
   return when (op.value) {
    "*" -> AST.Mul(x, y)
    "/" -> AST.Div(x, y)
    "%" -> AST.Mod(x, y)
    else -> throw Skip("Unexpected Behaviour")
   }
  }

  val mark = this.mark()
  _0()?.let { return it }
  this.mark(mark)
  unit()?.let { return it }
  this.mark(mark)
  return null
 }

 private fun unit(): AST? {
  fun _0(): AST? {
   val x = group() ?: return null
   expect(Token.UNIT) ?: return null
   val y = group() ?: throw Skip("Expected expression after \"^\"")
   return AST.Pow(x, y)
  }

  val mark = this.mark()
  _0()?.let { return it }
  this.mark(mark)
  group()?.let { return it }
  this.mark(mark)
  return null
 }

 private fun group(): AST? {
  fun _0(): AST? {
   expect(Token.SQRT) ?: return null
   expect(Token.OPEN) ?: throw Skip("Expected open bracket after square root")
   val x = product() ?: throw Skip("Expected expression inside of square root")
   expect(Token.CLOSE) ?: throw Skip("Expected close bracket after expression")
   return AST.Sqrt(x)
  }

  fun _1(): AST? {
   expect(Token.OPEN) ?: return null
   val x = product() ?: throw Skip("Expected expression inside of group")
   expect(Token.CLOSE) ?: throw Skip("Expected close bracket of group")
   return x
  }

  val mark = this.mark()
  _0()?.let { return it }
  this.mark(mark)
  _1()?.let { return it }
  this.mark(mark)
  expect(Token.NUMBER)?.let { return AST.Number(it) }
  return null
 }

 private fun _pow(x: Int, y: Int): Int {
  var result = 1
  for (i in 1..y)
   result *= x
  return result
 }

 private fun interpret(node: AST): Int {
  return when (node) {
   is AST.Add -> interpret(node.x) + interpret(node.y)
   is AST.Sub -> interpret(node.x) - interpret(node.y)
   is AST.Mul -> interpret(node.x) * interpret(node.y)
   is AST.Div -> interpret(node.x) / interpret(node.y)
   is AST.Mod -> interpret(node.x) % interpret(node.y)
   is AST.Pow -> this._pow(interpret(node.x), interpret(node.y))
   is AST.Sqrt -> sqrt(interpret(node.x).toDouble()).toInt()
   is AST.Number -> node.value
  }
 }

 fun reset() {
  this.tokens.clear()
  this.pos = 0
 }

 fun calculate(expression: String): Int {
  tokenize(expression)
  return interpret(
   product()?.also {
    reset()
    if (this.pos != this.tokens.size)
     throw Skip(
      "Wrong expression \"${
       this.tokens.takeLast(
        this.tokens.size - this.pos
       ).map { it.value }.joinToString(" ")
      }\""
     )
   } ?: throw Skip("Wrong expression \"$expression\"")
  )
 }
}


fun main() {
 while (true) {
  print("Expression: ")
  try {
   println(Calculator.calculate(readln()))
  } catch (e: Calculator.Skip) {
   println(e.message)
   Calculator.reset()
  }
 }
}