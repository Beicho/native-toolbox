package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * 手机号归属地:49 万号段本地库(assets/phone.dat)二分查找,完全离线。
 */
private object PhoneDb {
    private var data: ByteArray? = null
    private var indexOffset = 0
    private var total = 0

    @Synchronized
    fun ensureLoaded(context: android.content.Context) {
        if (data != null) return
        val d = context.assets.open("phone.dat").readBytes()
        data = d
        indexOffset = readIntLE(d, 4)
        total = (d.size - indexOffset) / 9
    }

    private fun readIntLE(d: ByteArray, off: Int): Int =
        (d[off].toInt() and 0xFF) or ((d[off + 1].toInt() and 0xFF) shl 8) or
            ((d[off + 2].toInt() and 0xFF) shl 16) or ((d[off + 3].toInt() and 0xFF) shl 24)

    data class Info(val province: String, val city: String, val zip: String, val areaCode: String, val isp: String)

    fun lookup(phone: String): Info? {
        val d = data ?: return null
        val seg = phone.take(7).toIntOrNull() ?: return null
        var lo = 0
        var hi = total - 1
        while (lo <= hi) {
            val mid = (lo + hi) / 2
            val off = indexOffset + mid * 9
            val num = readIntLE(d, off)
            when {
                num == seg -> {
                    val recOff = readIntLE(d, off + 4)
                    var end = recOff
                    while (end < d.size && d[end] != 0.toByte()) end++
                    val parts = String(d, recOff, end - recOff, Charsets.UTF_8).split("|")
                    val isp = when (d[off + 8].toInt()) {
                        1 -> "中国移动"; 2 -> "中国联通"; 3 -> "中国电信"
                        4 -> "电信虚拟运营商"; 5 -> "联通虚拟运营商"; 6 -> "移动虚拟运营商"
                        7 -> "中国广电"; 8 -> "广电虚拟运营商"
                        else -> "未知运营商"
                    }
                    return Info(
                        parts.getOrElse(0) { "" }, parts.getOrElse(1) { "" },
                        parts.getOrElse(2) { "" }, parts.getOrElse(3) { "" }, isp
                    )
                }
                num < seg -> lo = mid + 1
                else -> hi = mid - 1
            }
        }
        return null
    }
}

// 车牌省份简称
private val PLATE_PROVINCE = mapOf(
    '京' to "北京", '津' to "天津", '冀' to "河北", '晋' to "山西", '蒙' to "内蒙古",
    '辽' to "辽宁", '吉' to "吉林", '黑' to "黑龙江", '沪' to "上海", '苏' to "江苏",
    '浙' to "浙江", '皖' to "安徽", '闽' to "福建", '赣' to "江西", '鲁' to "山东",
    '豫' to "河南", '鄂' to "湖北", '湘' to "湖南", '粤' to "广东", '桂' to "广西",
    '琼' to "海南", '渝' to "重庆", '川' to "四川", '贵' to "贵州", '云' to "云南",
    '藏' to "西藏", '陕' to "陕西", '甘' to "甘肃", '青' to "青海", '宁' to "宁夏",
    '新' to "新疆", '港' to "香港", '澳' to "澳门", '使' to "使馆", '领' to "领馆",
)

// 各省城市字母(地级市代号,省会一般是 A)
private val PLATE_CITY = mapOf(
    "冀" to "A石家庄 B唐山 C秦皇岛 D邯郸 E邢台 F保定 G张家口 H承德 J沧州 R廊坊 T衡水",
    "晋" to "A太原 B大同 C阳泉 D长治 E晋城 F朔州 H忻州 J吕梁 K晋中 L临汾 M运城",
    "蒙" to "A呼和浩特 B包头 C乌海 D赤峰 E呼伦贝尔 F兴安盟 G通辽 H锡林郭勒 J乌兰察布 K鄂尔多斯 L巴彦淖尔 M阿拉善",
    "辽" to "A沈阳 B大连 C鞍山 D抚顺 E本溪 F丹东 G锦州 H营口 J阜新 K辽阳 L盘锦 M铁岭 N朝阳 P葫芦岛",
    "吉" to "A长春 B吉林 C四平 D辽源 E通化 F白山 G白城 H松原 J延边",
    "黑" to "A哈尔滨 B齐齐哈尔 C牡丹江 D佳木斯 E大庆 F伊春 G鸡西 H鹤岗 J双鸭山 K七台河 L绥化 M黑河 N大兴安岭",
    "苏" to "A南京 B无锡 C徐州 D常州 E苏州 F南通 G连云港 H淮安 J盐城 K扬州 L镇江 M泰州 N宿迁",
    "浙" to "A杭州 B宁波 C温州 D绍兴 E湖州 F嘉兴 G金华 H衢州 J台州 K丽水 L舟山",
    "皖" to "A合肥 B芜湖 C蚌埠 D淮南 E马鞍山 F淮北 G铜陵 H安庆 J黄山 K阜阳 L宿州 M滁州 N六安 P宣城 R池州 S亳州",
    "闽" to "A福州 B莆田 C泉州 D厦门 E漳州 F龙岩 G三明 H南平 J宁德",
    "赣" to "A南昌 B赣州 C宜春 D吉安 E上饶 F抚州 G九江 H景德镇 J萍乡 K新余 L鹰潭",
    "鲁" to "A济南 B青岛 C淄博 D枣庄 E东营 F烟台 G潍坊 H济宁 J泰安 K威海 L日照 M滨州 N德州 P聊城 Q临沂 R菏泽 U青岛(增) S莱芜",
    "豫" to "A郑州 B开封 C洛阳 D平顶山 E新乡 F安阳 G焦作 H鹤壁 J濮阳 K许昌 L漯河 M三门峡 N商丘 P周口 Q驻马店 R南阳 S信阳 U济源",
    "鄂" to "A武汉 B黄石 C十堰 D荆州 E宜昌 F襄阳 G鄂州 H荆门 J黄冈 K孝感 L咸宁 M仙桃 N潜江 P神农架 Q天门 R恩施 S随州",
    "湘" to "A长沙 B株洲 C湘潭 D衡阳 E邵阳 F岳阳 G张家界 H益阳 J常德 K娄底 L郴州 M永州 N怀化 U湘西",
    "粤" to "A广州 B深圳 C珠海 D汕头 E佛山 F韶关 G湛江 H肇庆 J江门 K茂名 L惠州 M梅州 N汕尾 P河源 Q阳江 R清远 S东莞 T中山 U潮州 V揭阳 W云浮 X顺德(佛山) Y南海(佛山)",
    "桂" to "A南宁 B柳州 C桂林 D梧州 E北海 F崇左 G来宾 H贺州 J玉林 K防城港 L百色 M河池 N钦州 P贵港",
    "川" to "A成都 B绵阳 C自贡 D攀枝花 E泸州 F德阳 H广元 J遂宁 K内江 L乐山 M资阳 Q宜宾 R南充 S达州 T雅安 U阿坝 V甘孜 W凉山 X广安 Y巴中 Z眉山",
    "贵" to "A贵阳 B六盘水 C遵义 D铜仁 E黔西南 F毕节 G安顺 H黔东南 J黔南",
    "云" to "A昆明 B东川(昆明) C昭通 D曲靖 E楚雄 F玉溪 G红河 H文山 J普洱 K西双版纳 L大理 M保山 N德宏 P丽江 Q怒江 R迪庆 S临沧",
    "陕" to "A西安 B铜川 C宝鸡 D咸阳 E渭南 F汉中 G安康 H商洛 J延安 K榆林 V杨凌",
    "甘" to "A兰州 B嘉峪关 C金昌 D白银 E天水 F酒泉 G张掖 H武威 J定西 K陇南 L平凉 M庆阳 N临夏 P甘南",
    "青" to "A西宁 B海东 C海北 D黄南 E海南州 F果洛 G玉树 H海西",
    "宁" to "A银川 B石嘴山 C吴忠 D固原 E中卫",
    "新" to "A乌鲁木齐 B昌吉 C石河子 D奎屯 E博尔塔拉 F伊犁 G塔城 H阿勒泰 J克拉玛依 K吐鲁番 L哈密 M巴音郭楞 N阿克苏 P克孜勒苏 Q喀什 R和田",
    "琼" to "A海口 B三亚 C琼北地区 D琼南地区 E洋浦",
).mapKeys { it.key[0] }

private fun plateLookup(text: String): List<Pair<String, String>> {
    val t = text.trim().uppercase()
    if (t.isEmpty()) return emptyList()
    val prov = t[0]
    val provName = PLATE_PROVINCE[prov] ?: return listOf("省份" to "认不出「$prov」这个简称")
    val out = mutableListOf("省份" to provName)
    if (t.length >= 2 && t[1] in 'A'..'Z') {
        val cities = PLATE_CITY[prov]
        if (cities == null) {
            // 直辖市:字母不分城市
            if (prov in listOf('京', '津', '沪', '渝', '藏', '港', '澳', '使', '领')) out.add("城市" to provName)
            else out.add("城市" to "字母 ${t[1]}(该省未收录明细)")
        } else {
            val hit = cities.split(' ').firstOrNull { it.startsWith(t[1]) }
            out.add("城市" to (hit?.drop(1) ?: "字母 ${t[1]} 未收录,可能是新增代号"))
        }
    }
    if (t.length >= 3) {
        val body = t.substring(2)
        if (body.contains('D') || body.contains('F')) {
            if (t.length == 8 || body.firstOrNull() in listOf('D', 'F')) out.add("类型" to "新能源车牌(D 纯电 / F 混动)")
        }
        if (t.length == 8) out.add("位数" to "8 位,新能源车")
    }
    return out
}

// 省级行政区划(身份证前两位)
private val ID_PROVINCE = mapOf(
    11 to "北京", 12 to "天津", 13 to "河北", 14 to "山西", 15 to "内蒙古",
    21 to "辽宁", 22 to "吉林", 23 to "黑龙江", 31 to "上海", 32 to "江苏",
    33 to "浙江", 34 to "安徽", 35 to "福建", 36 to "江西", 37 to "山东",
    41 to "河南", 42 to "湖北", 43 to "湖南", 44 to "广东", 45 to "广西", 46 to "海南",
    50 to "重庆", 51 to "四川", 52 to "贵州", 53 to "云南", 54 to "西藏",
    61 to "陕西", 62 to "甘肃", 63 to "青海", 64 to "宁夏", 65 to "新疆",
    71 to "台湾", 81 to "香港", 82 to "澳门",
)

private fun idLookup(text: String): List<Pair<String, String>> {
    val t = text.trim().uppercase()
    if (t.length != 18) return listOf("提示" to "身份证是 18 位,现在 ${t.length} 位")
    if (!t.dropLast(1).all { it.isDigit() } || (!t.last().isDigit() && t.last() != 'X'))
        return listOf("提示" to "格式不对:前 17 位数字,最后一位数字或 X")
    val out = mutableListOf<Pair<String, String>>()
    // 校验位
    val weights = intArrayOf(7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2)
    val checkMap = "10X98765432"
    val sum = (0 until 17).sumOf { (t[it] - '0') * weights[it] }
    val expect = checkMap[sum % 11]
    out.add("校验" to if (expect == t[17]) "通过,是合法号码" else "不通过!这个号码是假的(应为 $expect)")
    out.add("省份" to (ID_PROVINCE[t.take(2).toInt()] ?: "未知省份"))
    val y = t.substring(6, 10).toInt()
    val m = t.substring(10, 12).toInt()
    val d = t.substring(12, 14).toInt()
    out.add("出生" to "$y 年 $m 月 $d 日")
    val now = Calendar.getInstance()
    var age = now.get(Calendar.YEAR) - y
    if (now.get(Calendar.MONTH) + 1 < m || (now.get(Calendar.MONTH) + 1 == m && now.get(Calendar.DAY_OF_MONTH) < d)) age--
    out.add("年龄" to "$age 岁")
    out.add("性别" to if ((t[16] - '0') % 2 == 1) "男" else "女")
    return out
}

@Composable
fun PhoneLocationToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    var mode by rememberSaveable { mutableStateOf(0) }
    var input by rememberSaveable { mutableStateOf("") }
    var rows by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { PhoneDb.ensureLoaded(context) }
        loaded = true
    }

    fun query() {
        rows = when (mode) {
            0 -> {
                val digits = input.filter { it.isDigit() }
                if (digits.length < 7) listOf("提示" to "手机号至少输前 7 位")
                else {
                    val info = PhoneDb.lookup(digits)
                    if (info == null) listOf("提示" to "没查到这个号段,可能是新放号段")
                    else listOfNotNull(
                        "归属地" to (info.province + (if (info.city != info.province) " " + info.city else "")),
                        "运营商" to info.isp,
                        ("区号" to info.areaCode).takeIf { info.areaCode.isNotBlank() },
                        ("邮编" to info.zip).takeIf { info.zip.isNotBlank() },
                    )
                }
            }
            1 -> plateLookup(input)
            else -> idLookup(input)
        }
    }

    ToolScaffold {
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(listOf("手机号", "车牌", "身份证"), mode, { mode = it; rows = emptyList() }, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    IosTextField(
                        input, { input = it }, Modifier.fillMaxWidth(),
                        placeholder = listOf("138001380 前7位即可", "粤B12345", "18 位身份证号")[mode]
                    )
                    Spacer(Modifier.height(10.dp))
                    SolidButton(onClick = { query() }, Modifier.fillMaxWidth(), enabled = loaded && input.isNotBlank()) {
                        Text(if (loaded) "查询" else "号段库加载中…")
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("49 万号段库存在本机,查询不联网不留痕", style = MaterialTheme.typography.bodySmall, color = palette.tertiaryLabel)
                }
            }
        }
        item {
            if (rows.isNotEmpty()) {
                GroupedCard {
                    rows.forEachIndexed { i, (k, v) ->
                        KeyValueRow(k, v)
                        if (i != rows.lastIndex) RowDivider()
                    }
                }
            }
        }
    }
}
