import org.codegen.ArgsParser
import org.codegen.Builder

fun main(args: Array<String>) {
    val parser = ArgsParser(args)
    parser.parse().ifPresent { params -> Builder(params).build().also { print(it) } }
}
