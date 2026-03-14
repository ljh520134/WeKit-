package moe.ouom.wekit.hooks.items.chat

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Typeface
import android.text.Html
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.TypedValue
import android.view.View
import androidx.compose.material3.Text
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withTranslation
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.extension.toClass
import dev.ujhhgtg.nameof.nameof
import io.noties.markwon.Markwon
import io.noties.markwon.ext.latex.JLatexMathPlugin
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin
import moe.ouom.wekit.core.dsl.dexClass
import moe.ouom.wekit.core.model.ClickableHookItem
import moe.ouom.wekit.dexkit.intf.IResolvesDex
import moe.ouom.wekit.hooks.core.annotation.HookItem
import moe.ouom.wekit.hooks.sdk.base.WeMessageApi
import moe.ouom.wekit.hooks.sdk.base.model.MessageInfo
import moe.ouom.wekit.ui.content.AlertDialogContent
import moe.ouom.wekit.ui.content.TextButton
import moe.ouom.wekit.ui.utils.showComposeDialog
import moe.ouom.wekit.utils.replaceEmojis
import org.luckypray.dexkit.DexKitBridge

@HookItem(path = "聊天/Markdown 渲染", desc = "渲染 Markdown 消息")
object MarkdownRendering : ClickableHookItem(), IResolvesDex {

    private val TAG = nameof(MarkdownRendering)

    private const val KEY_USE_MARKWON = "use_markwon"

    private lateinit var markwon: Markwon

    private external fun convertMarkdownToHtmlNative(markdown: String): String?

    override fun onLoad() {
        "com.tencent.mm.ui.widget.MMNeat7extView".toClass().asResolver().apply {
            // path 2: draw HTML on canvas myself
            firstMethod { name = "onDraw" }
                .hookBefore { param ->
                    val neatTextView = param.thisObject as View
                    if (!::markwon.isInitialized) {
                        markwon = Markwon.builder(neatTextView.context)
                            .usePlugin(MarkwonInlineParserPlugin.create())
                            .usePlugin(JLatexMathPlugin.create(15f) {
                                it.inlinesEnabled(true)
                            })
                            .usePlugin(StrikethroughPlugin.create())
                            .build()
                    }

                    // path 1: disable WeChat's neat rendering and replace TextView
                    // doesn't seem to work, idk why
//                    neatTextView.asResolver()
//                        .field {
//                            type = Boolean::class
//                            modifiers { !it.contains(Modifiers.STATIC) }
//                            superclass()
//                        }[2]
//                        .set(true)

                    var origText = (neatTextView.asResolver()
                        .firstField {
                            type = CharSequence::class
                            superclass()
                        }.get()!! as CharSequence).toString()
                    if (origText.isBlank()) return@hookBefore
                    origText = origText.replaceEmojis()

                    val msgInfo = MessageInfo(
                        neatTextView.tag.asResolver()
                        .firstField {
                            type = classMsgInfoWrapper.clazz
                            superclass()
                        }
                        .get()!!.asResolver()
                        .firstField {
                            superclass()
                        }
                        .get()!!.asResolver()
                        .firstField { type = WeMessageApi.classMsgInfo.clazz }
                        .get()!!)
                    if (!msgInfo.isText) return@hookBefore
                    val isSelfSender = msgInfo.isSelfSender()

                    val canvas = param.args[0] as Canvas
                    val context = neatTextView.context

                    val isDarkMode = (context.resources.configuration.uiMode and
                            Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

                    val textPaint = TextPaint().apply {
                        color = if (isDarkMode && !isSelfSender) "#CDCDCD".toColorInt() else "#282828".toColorInt()

                        val spSize = 17f
                        textSize = TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_SP,
                            spSize,
                            context.resources.displayMetrics
                        )

                        isAntiAlias = true
                        typeface = Typeface.DEFAULT
                    }

                    // Respecting bubble constraints
                    val horizontalPadding = neatTextView.paddingLeft + neatTextView.paddingRight
                    val maxWidth = neatTextView.width - horizontalPadding

                    if (maxWidth <= 0) return@hookBefore

//                    val htmlString = convertMarkdownToHtmlNative(origText)
//
//                    if (htmlString != null) {
//                        WeLogger.d(TAG, "html string:\n$htmlString")
//                        drawHtmlOnCanvas(
//                            canvas,
//                            htmlString,
//                            neatTextView.paddingLeft.toFloat(),
//                            neatTextView.paddingTop.toFloat(),
//                            maxWidth,
//                            textPaint
//                        )
//                        param.result = null
//                    }
//                    else {
//                        WeLogger.e(TAG, "convertMarkdownToHtmlNative returned nullptr, falling back to original rendering")
//                    }
                    drawMarkdownWithMarkwon(
                        canvas,
                    origText,
                        neatTextView.paddingLeft.toFloat(),
                        neatTextView.paddingTop.toFloat(),
                        maxWidth,
                        textPaint)
                    param.result = null
                }

            // path 1: disable WeChat's neat rendering and replace TextView
            // doesn't seem to work, idk why
//            firstMethod {
//                name = "getWrappedTextView"
//                superclass()
//            }.hookAfter { param ->
//                val neatTextView = param.thisObject as View
//                if (neatTextView.javaClass.simpleName != "MMNeat7extView") return@hookAfter
//
//                val origText = neatTextView.asResolver()
//                    .firstField {
//                        type = CharSequence::class
//                        superclass()
//                    }.get()!! as CharSequence
//                if (origText.isEmpty()) return@hookAfter
//                WeLogger.d(TAG, "original text: $origText")
//                WeLogger.d(TAG, "tag: ${neatTextView.tag?.javaClass?.name ?: "null"}")
//
//                val textView = TextView(neatTextView.context).apply {
//                    layoutParams = LinearLayout.LayoutParams(
//                        LinearLayout.LayoutParams.MATCH_PARENT,
//                        LinearLayout.LayoutParams.WRAP_CONTENT
//                    )
//                    textSize = 18f
//                }
//
//                val content = "Bold, Italic, and Underlined"
//                val spannable = SpannableString(content).apply {
//                    // Bold "Bold" (indices 0-4)
//                    setSpan(StyleSpan(Typeface.BOLD), 0, 4, 0)
//
//                    // Italic "Italic" (indices 6-12)
//                    setSpan(StyleSpan(Typeface.ITALIC), 6, 12, 0)
//
//                    // Underline "Underlined" (indices 17-28)
//                    setSpan(UnderlineSpan(), 17, 28, 0)
//
//                    // Add a color pop to "Underlined"
//                    setSpan(ForegroundColorSpan(Color.MAGENTA), 17, 28, 0)
//                }
//
//                textView.text = spannable
//                textView.setTextColor(Color.WHITE)
//
//                param.result = textView
//            }

            // path 1: disable WeChat's neat rendering and replace TextView
            // doesn't seem to work, idk why
//            firstMethod { returnType = Boolean::class }
//                .hookBefore { param ->
//                    // disable WeChat's neat rendering
//                    param.result = true
//                }
        }
    }

    private fun drawMarkdownWithMarkwon(
        canvas: Canvas,
        markdownString: String,
        x: Float,
        y: Float,
        maxWidth: Int,
        textPaint: TextPaint
    ) {
        val node = markwon.parse(markdownString)
        val spanned = markwon.render(node)
        val staticLayout = StaticLayout.Builder
            .obtain(spanned, 0, spanned.length, textPaint, maxWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.1f)
            .setIncludePad(true)
            .build()

        canvas.withTranslation(x, y) {
            staticLayout.draw(this)
        }
    }

    private fun drawHtmlOnCanvas(
        canvas: Canvas,
        htmlString: String,
        x: Float,
        y: Float,
        maxWidth: Int,
        textPaint: TextPaint
    ) {
//        // 1. Convert HTML string to Spanned text
//        val spannedText = Html.fromHtml(htmlString,
//            Html.FROM_HTML_MODE_LEGACY)
//
//        // 2. Create a StaticLayout to handle line wrapping and rendering
//        val staticLayout = StaticLayout.Builder
//            .obtain(spannedText, 0, spannedText.length, textPaint, maxWidth)
//            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
//            .setLineSpacing(0f, 1.1f)
//            .setIncludePad(true)
//            .build()
//
//        // 3. Draw the layout onto the canvas
//        canvas.withTranslation(x, y) {
//            staticLayout.draw(this)
//        }

        // prevent setting font size

        // 1. Convert HTML to Spanned
        val spannedText = Html.fromHtml(htmlString, Html.FROM_HTML_MODE_COMPACT)

        // 2. Create a SpannableStringBuilder to mutate the spans
//        val spannable = SpannableStringBuilder(spannedText)

//        // 3. Remove RelativeSizeSpans (common in headings) and AbsoluteSizeSpans
//        val relativeSpans = spannable.getSpans(0, spannable.length,
//            RelativeSizeSpan::class.java)
//        for (span in relativeSpans) {
//            spannable.removeSpan(span)
//        }
//
//        val absoluteSpans = spannable.getSpans(0, spannable.length,
//            AbsoluteSizeSpan::class.java)
//        for (span in absoluteSpans) {
//            spannable.removeSpan(span)
//        }

        // 4. Build the layout using the cleaned 'spannable'
        val staticLayout = StaticLayout.Builder
            .obtain(spannedText, 0, spannedText.length, textPaint, maxWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.1f)
            .setIncludePad(true)
            .build()

        canvas.withTranslation(x, y) {
            staticLayout.draw(this)
        }
    }

    override fun onClick(context: Context) {
        showComposeDialog(context) {
            AlertDialogContent(
                title = { Text("Markdown 渲染") },
                text = { Text("预留设置页") },
                confirmButton = { TextButton(onDismiss) { Text("关闭") } }
            )
        }
    }

    private val classMsgInfoWrapper by dexClass()

    override fun resolveDex(dexKit: DexKitBridge): Map<String, String> {
        val descriptors = mutableMapOf<String, String>()

        classMsgInfoWrapper.find(dexKit, descriptors) {
            matcher {
                usingEqStrings("params", "other", "null cannot be cast to non-null type com.tencent.mm.storage.MsgInfo", "msgInfo")
            }
        }

        return descriptors
    }
}
