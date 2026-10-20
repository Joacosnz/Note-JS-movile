import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable

@Composable
fun TestBTF() {
    BasicTextField(
        value = "",
        onValueChange = {},
        // inlineContent = emptyMap<String, InlineTextContent>()
    )
}
