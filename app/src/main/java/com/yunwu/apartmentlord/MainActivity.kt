package com.yunwu.apartmentlord

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import kotlin.math.max
import kotlin.random.Random

data class Room(val id: Int, val name: String, val rent: Int, val cost: Int, var built: Boolean = false, var tenant: String = "")
data class Resident(val name: String, val job: String, val combat: Int, val work: Int, val rent: Int, val skill: String, var hp: Int = 100, var mission: String = "")
data class Facility(val id: String, val name: String, val cost: Int, val icon: String, var level: Int = 0)

data class Mission(val title: String, val description: String, val reward: String)

private val Sand = Color(0xFFF2E7D5)
private val Dust = Color(0xFFD8C3A5)
private val Brown = Color(0xFF795548)
private val Dark = Color(0xFF2F2924)
private val Green = Color(0xFF657C5A)
private val Red = Color(0xFFB65F52)
private val Blue = Color(0xFF5F8296)
private val Gold = Color(0xFFC69755)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Game() }
    }
}

@Composable
fun Game() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("apartment_lord_v13", Context.MODE_PRIVATE) }

    var day by rememberSaveable { mutableIntStateOf(prefs.getInt("day", 1)) }
    var money by rememberSaveable { mutableIntStateOf(prefs.getInt("money", 1200)) }
    var power by rememberSaveable { mutableIntStateOf(prefs.getInt("power", 35)) }
    var water by rememberSaveable { mutableIntStateOf(prefs.getInt("water", 45)) }
    var food by rememberSaveable { mutableIntStateOf(prefs.getInt("food", 55)) }
    var medicine by rememberSaveable { mutableIntStateOf(prefs.getInt("medicine", 8)) }
    var materials by rememberSaveable { mutableIntStateOf(prefs.getInt("materials", 20)) }
    var level by rememberSaveable { mutableIntStateOf(prefs.getInt("level", 1)) }
    var reputation by rememberSaveable { mutableIntStateOf(prefs.getInt("reputation", 10)) }
    var security by rememberSaveable { mutableIntStateOf(prefs.getInt("security", 15)) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var message by rememberSaveable { mutableStateOf(prefs.getString("message", "系统：欢迎来到 A-17 安全社区。") ?: "系统：欢迎来到 A-17 安全社区。") }
    var chapter by rememberSaveable { mutableIntStateOf(prefs.getInt("chapter", 1)) }
    var shopFlash by rememberSaveable { mutableStateOf("") }

    val rooms = remember {
        mutableStateListOf(*List(12) { i ->
            val id = i + 1
            val built = prefs.getBoolean("room_$id", id == 1)
            Room(id, if (id <= 4) "废墟单间" else if (id <= 8) "标准公寓" else "高级公寓", if (id <= 4) 70 else if (id <= 8) 110 else 170, if (id <= 4) 260 else if (id <= 8) 380 else 600, built, prefs.getString("room_tenant_$id", "") ?: "")
        }.toTypedArray())
    }

    val residents = remember {
        mutableStateListOf<Resident>().apply {
            val all = residentPool()
            all.forEach { r -> if (prefs.getBoolean("resident_${r.name}", false)) add(r.copy(hp = prefs.getInt("hp_${r.name}", 100), mission = prefs.getString("mission_${r.name}", "") ?: "")) }
        }
    }

    val facilities = remember {
        mutableStateListOf(
            Facility("generator", "备用发电机", 500, "⚡", prefs.getInt("fac_generator", 0)),
            Facility("tank", "净水塔", 450, "💧", prefs.getInt("fac_tank", 0)),
            Facility("clinic", "小型诊所", 700, "🏥", prefs.getInt("fac_clinic", 0)),
            Facility("wall", "社区围墙", 900, "🧱", prefs.getInt("fac_wall", 0)),
            Facility("warehouse", "物资仓库", 650, "📦", prefs.getInt("fac_warehouse", 0))
        )
    }

    fun save() {
        prefs.edit().apply {
            putInt("day", day); putInt("money", money); putInt("power", power); putInt("water", water); putInt("food", food)
            putInt("medicine", medicine); putInt("materials", materials); putInt("level", level); putInt("reputation", reputation)
            putInt("security", security); putString("message", message); putInt("chapter", chapter)
            rooms.forEach { putBoolean("room_${it.id}", it.built); putString("room_tenant_${it.id}", it.tenant) }
            residentPool().forEach { r -> putBoolean("resident_${r.name}", residents.any { it.name == r.name }) }
            residents.forEach { putInt("hp_${it.name}", it.hp); putString("mission_${it.name}", it.mission) }
            facilities.forEach { putInt("fac_${it.id}", it.level) }
            apply()
        }
    }

    fun notify(text: String) { message = text; save() }

    fun buildRoom(id: Int) {
        val index = rooms.indexOfFirst { it.id == id }
        if (index < 0) return
        val r = rooms[index]
        if (r.built) return
        if (money < r.cost) { notify("💰 修复 ${r.name} 需要 ${r.cost} 废土币。"); return }
        money -= r.cost; materials = max(0, materials - 2); rooms[index] = r.copy(built = true)
        reputation += 2
        notify("🏠 ${r.name} 修复完成！房源增加，社区声望 +2。")
    }

    fun recruit(id: Int) {
        val index = rooms.indexOfFirst { it.id == id }
        if (index < 0) return
        val room = rooms[index]
        if (!room.built || room.tenant.isNotEmpty()) return
        if (residents.size >= level * 4) { notify("🔒 Lv.$level 最多容纳 ${level * 4} 名居民。"); return }
        val candidate = residentPool().firstOrNull { c -> residents.none { it.name == c.name } }
        if (candidate == null) { notify("社区暂时没有新的求租者。"); return }
        residents.add(candidate)
        rooms[index] = room.copy(tenant = candidate.name)
        reputation += 3
        notify("👤 ${candidate.name} 入住！${candidate.job}：${candidate.skill}。每日租金 +${candidate.rent}。")
    }

    fun upgradeFacility(id: String) {
        val index = facilities.indexOfFirst { it.id == id }
        if (index < 0) return
        val f = facilities[index]
        if (f.level >= 3) { notify("该设施已经达到 Lv.3。"); return }
        val cost = f.cost * (f.level + 1)
        if (money < cost || materials < 5) { notify("🧱 升级需要 ${cost} 废土币 + 5 建材。"); return }
        money -= cost; materials -= 5
        facilities[index] = f.copy(level = f.level + 1)
        when (id) {
            "generator" -> power += 12
            "tank" -> water += 15
            "clinic" -> medicine += 4
            "wall" -> security += 10
            "warehouse" -> materials += 8
        }
        notify("🔧 ${f.name} 升至 Lv.${f.level + 1}！社区能力得到提升。")
    }

    fun expedition(residentName: String, place: String) {
        val index = residents.indexOfFirst { it.name == residentName }
        if (index < 0) return
        val r = residents[index]
        if (r.mission.isNotEmpty()) { notify("${r.name} 已经在外出任务中。"); return }
        if (food < 3 || water < 2) { notify("🥫 外出至少需要 3 食物和 2 水。"); return }
        food -= 3; water -= 2
        residents[index] = r.copy(mission = place)
        notify("🚚 ${r.name} 已前往$place 搜集物资，下一天回来。")
    }

    fun nextDay() {
        var income = residents.sumOf { it.rent }
        val upkeep = residents.size * 5
        money += income - upkeep
        food = max(0, food - residents.size * 2)
        water = max(0, water - residents.size * 2)
        power = max(0, power - max(1, residents.size / 3))

        residents.forEachIndexed { i, r ->
            if (r.mission.isNotEmpty()) {
                val roll = Random.nextInt(100)
                val gain = when (r.mission) {
                    "废弃超市" -> { food += 8 + r.work / 20; materials += 2; "🍞 找回了食物" }
                    "旧医院" -> { medicine += 4 + r.work / 30; "💊 找回了药品" }
                    "废弃仓库" -> { materials += 8 + r.work / 15; "🧱 找回了建材" }
                    else -> "📦 找回了一些物资"
                }
                if (roll < 12 && security < 35) {
                    residents[i] = r.copy(hp = max(25, r.hp - 20), mission = "")
                    medicine = max(0, medicine - 1)
                    message = "⚠️ ${r.name} 外出受伤了！消耗 1 药品。"
                } else {
                    residents[i] = r.copy(mission = "")
                    message = "${message}\n$gain。${r.name} 已安全返回。"
                }
            }
        }

        day += 1
        val event = Random.nextInt(100)
        val wallLevel = facilities.first { it.id == "wall" }.level
        val clinicLevel = facilities.first { it.id == "clinic" }.level
        when {
            day == 3 -> { chapter = max(chapter, 2); reputation += 5; message = "📖 第2章：第一批邻居来了。社区开始真正像一个家。" }
            day == 7 -> { chapter = max(chapter, 3); materials += 15; message = "📖 第3章：附近幸存者听说 A-17 有住处，开始主动投奔。" }
            event < 15 -> {
                val damage = max(2, 14 - wallLevel * 4)
                security = max(0, security - damage)
                if (security < 10) food = max(0, food - 4)
                message = "☣️ 尸潮来袭！围墙等级 Lv.$wallLevel，社区安全值变化 -$damage。"
            }
            event < 28 -> { money += 120; food += 6; water += 6; reputation += 2; message = "🚛 政府补给车经过 A-17！获得 120 废土币、食物和水。" }
            event < 40 -> { materials += 10; message = "🛠️ 你在社区地下室发现一批旧建材，建材 +10。" }
            event < 52 -> { reputation += 4; message = "🏠 一群幸存者参观了社区，声望 +4。" }
            event < 64 -> { power = max(0, power - 6); message = "⚡ 临时电网故障，电力 -6。发电机可以缓解问题。" }
            event < 74 -> { food += 5; message = "🌱 居民把废弃屋顶改成菜地，食物 +5。" }
            else -> message = "🌙 第 $day 天平稳结束。今日租金收入 $income，生活支出 $upkeep。"
        }
        if (food == 0) message += "\n⚠️ 食物见底，居民满意度正在下降。"
        if (water == 0) message += "\n⚠️ 水源不足，下一天可能发生居民生病。"
        if (power == 0) message += "\n⚠️ 电力归零，部分设施效率下降。"

        if (money >= level * 2200 && level < 6) {
            level += 1
            security += 4
            reputation += 8
            message += "\n🎉 包租系统升级！现在是 Lv.$level，解锁更多居民容量。"
        }
        if (clinicLevel > 0) residents.forEachIndexed { i, r -> if (r.hp < 100) residents[i] = r.copy(hp = minOf(100, r.hp + clinicLevel * 8)) }
        save()
    }

    fun buy(item: String) {
        when (item) {
            "food" -> if (money >= 80) { money -= 80; food += 12; shopFlash = "购买 12 食物" } else shopFlash = "废土币不足"
            "water" -> if (money >= 70) { money -= 70; water += 12; shopFlash = "购买 12 水" } else shopFlash = "废土币不足"
            "medicine" -> if (money >= 120) { money -= 120; medicine += 3; shopFlash = "购买 3 药品" } else shopFlash = "废土币不足"
            "materials" -> if (money >= 100) { money -= 100; materials += 10; shopFlash = "购买 10 建材" } else shopFlash = "废土币不足"
        }
        notify("🛒 商店：$shopFlash")
    }

    MaterialTheme(colorScheme = lightColorScheme(primary = Brown, secondary = Green, background = Sand, surface = Color.White)) {
        Scaffold(
            bottomBar = {
                NavigationBar(containerColor = Color.White) {
                    navItem(0, tab, { tab = 0 }, Icons.Default.Home, "家园")
                    navItem(1, tab, { tab = 1 }, Icons.Default.People, "居民")
                    navItem(2, tab, { tab = 2 }, Icons.Default.Explore, "外出")
                    navItem(3, tab, { tab = 3 }, Icons.Default.Store, "商店")
                    navItem(4, tab, { tab = 4 }, Icons.Default.Settings, "系统")
                }
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().background(Sand).padding(padding)) {
                Header(day, level, money, food, water, power, reputation)
                when (tab) {
                    0 -> HomeScreen(rooms, facilities, message, ::buildRoom, ::recruit, ::upgradeFacility, ::nextDay)
                    1 -> ResidentScreen(residents)
                    2 -> ExpeditionScreen(residents, ::expedition)
                    3 -> ShopScreen(money, shopFlash, ::buy)
                    4 -> SystemScreen(level, day, money, reputation, security, materials, chapter)
                }
            }
        }
    }
}

@Composable
fun navItem(index: Int, selected: Int, onClick: () -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    NavigationBarItem(selected == index, onClick, icon = { Icon(icon, null) }, label = { Text(label) })
}

@Composable
fun Header(day: Int, level: Int, money: Int, food: Int, water: Int, power: Int, reputation: Int) {
    Box(Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp)).background(Color(0xFF3D3935))) {
        Canvas(Modifier.fillMaxSize()) {
            val p = Path().apply {
                moveTo(0f, size.height * .68f); lineTo(size.width * .24f, size.height * .43f); lineTo(size.width * .46f, size.height * .72f)
                lineTo(size.width * .7f, size.height * .42f); lineTo(size.width, size.height * .65f); lineTo(size.width, size.height); lineTo(0f, size.height); close()
            }
            drawPath(p, Color(0xFF514B45))
        }
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(Gold), contentAlignment = Alignment.Center) { Icon(Icons.Default.HomeWork, null, tint = Dark) }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("末世包租公", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Black)
                    Text("A-17 安全社区 · 第 $day 天", color = Color(0xFFE5D8C8), fontSize = 11.sp)
                }
                Surface(shape = RoundedCornerShape(30.dp), color = Green) { Text("Lv.$level", Modifier.padding(horizontal = 11.dp, vertical = 6.dp), color = Color.White, fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MiniStat("💰", money.toString()); MiniStat("🍖", food.toString()); MiniStat("💧", water.toString()); MiniStat("⚡", power.toString()); MiniStat("⭐", reputation.toString())
            }
        }
    }
}

@Composable fun MiniStat(icon: String, value: String) { Surface(shape = RoundedCornerShape(11.dp), color = Color(0xCCFFFFFF)) { Row(Modifier.padding(horizontal = 7.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { Text(icon, fontSize = 12.sp); Spacer(Modifier.width(3.dp)); Text(value, color = Dark, fontSize = 11.sp, fontWeight = FontWeight.Bold) } } }

@Composable
fun HomeScreen(rooms: List<Room>, facilities: List<Facility>, message: String, build: (Int) -> Unit, recruit: (Int) -> Unit, upgrade: (String) -> Unit, nextDay: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 18.dp)) {
        item { SectionTitle("社区总览", "把废墟变成末世里最安全、最舒服的出租社区。") }
        item { Notice(message) }
        item { SectionTitle("房源", "修复房屋后才能招租。不同房型租金不同。") }
        items(rooms.chunked(2), key = { row -> row.first().id }) { row ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { RoomCard(it, build, recruit, Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
        }
        item { SectionTitle("社区设施", "设施直接影响资源产量与尸潮防御。") }
        items(facilities, key = { it.id }) { FacilityCard(it, upgrade) }
        item {
            Button(nextDay, Modifier.fillMaxWidth().padding(12.dp), shape = RoundedCornerShape(17.dp), colors = ButtonDefaults.buttonColors(containerColor = Brown)) {
                Icon(Icons.Default.FastForward, null); Spacer(Modifier.width(8.dp)); Text("进入下一天 · 收租并处理事件")
            }
        }
    }
}

@Composable fun SectionTitle(title: String, sub: String) { Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp)) { Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Dark); Text(sub, fontSize = 11.sp, color = Color(0xFF75695F)) } }

@Composable fun Notice(text: String) { Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE1D0B7))) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(42.dp).clip(CircleShape).background(Brown), contentAlignment = Alignment.Center) { Icon(Icons.Default.Campaign, null, tint = Color.White) }; Spacer(Modifier.width(10.dp)); Text(text, Modifier.weight(1f), fontSize = 12.sp, color = Dark) } } }

@Composable
fun RoomCard(room: Room, build: (Int) -> Unit, recruit: (Int) -> Unit, modifier: Modifier) {
    val bg = if (room.tenant.isNotEmpty()) Color(0xFFDDE8D7) else if (room.built) Color(0xFFE8DDC8) else Color(0xFFC9B9A5)
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(containerColor = bg)) {
        Column(Modifier.padding(9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(11.dp)).background(if (room.built) Color(0xFFB99167) else Color(0xFF9B8976)), contentAlignment = Alignment.Center) {
                Icon(if (room.tenant.isNotEmpty()) Icons.Default.Person else if (room.built) Icons.Default.Key else Icons.Default.HomeRepairService, null, tint = Color.White, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.height(5.dp)); Text("#${room.id} ${room.name}", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(if (room.tenant.isNotEmpty()) room.tenant else if (room.built) "空置 · ${room.rent}/天" else "废墟 · 修复 ${room.cost}", fontSize = 9.sp, color = Color(0xFF65594F))
            Spacer(Modifier.height(4.dp))
            if (!room.built) TinyButton("修复", { build(room.id) }) else if (room.tenant.isEmpty()) TinyButton("招租", { recruit(room.id) }) else Text("已出租", fontSize = 10.sp, color = Green, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable fun TinyButton(text: String, click: () -> Unit) { Button(click, Modifier.fillMaxWidth().height(28.dp), contentPadding = PaddingValues(0.dp), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = Brown)) { Text(text, fontSize = 10.sp) } }

@Composable
fun FacilityCard(f: Facility, upgrade: (String) -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(13.dp)).background(Dust), contentAlignment = Alignment.Center) { Text(f.icon, fontSize = 23.sp) }
            Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(f.name, fontWeight = FontWeight.Bold); Text("等级 Lv.${f.level} · 下一级 ${f.cost * (f.level + 1)} 金 + 5 建材", fontSize = 10.sp, color = Color(0xFF75695F)) }
            TextButton({ upgrade(f.id) }) { Text(if (f.level >= 3) "满级" else "升级") }
        }
    }
}

@Composable
fun ResidentScreen(residents: List<Resident>) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        item { SectionTitle("社区居民", "租客不只是交房租的人，他们也是你的生产力与战斗力。") }
        if (residents.isEmpty()) item { EmptyBox("还没有居民。先去家园修复房间并招租。") }
        items(residents, key = { it.name }) { r ->
            Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(54.dp).clip(CircleShape).background(Dust), contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, tint = Brown, modifier = Modifier.size(30.dp)) }
                    Spacer(Modifier.width(11.dp)); Column(Modifier.weight(1f)) { Text(r.name, fontSize = 17.sp, fontWeight = FontWeight.Bold); Text(r.job + " · " + r.skill, fontSize = 11.sp, color = Color(0xFF75695F)); Text("⚔ ${r.combat}  🔧 ${r.work}  ❤️ ${r.hp}  💰 ${r.rent}/天", fontSize = 11.sp) }
                    if (r.mission.isNotEmpty()) Surface(shape = RoundedCornerShape(9.dp), color = Color(0xFFE7DED2)) { Text("外出", Modifier.padding(7.dp), fontSize = 10.sp, color = Brown) }
                }
            }
        }
    }
}

@Composable
fun ExpeditionScreen(residents: List<Resident>, send: (String, String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        item { SectionTitle("外出搜集", "派居民进入末世废墟。高工作能力能带回更多物资，但也会有受伤风险。") }
        if (residents.none { it.mission.isEmpty() }) item { EmptyBox("所有居民都在外出任务中。明天他们会回来。") }
        items(residents.filter { it.mission.isEmpty() }, key = { it.name }) { r ->
            Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), shape = RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(13.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Text(r.name, fontSize = 17.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.width(8.dp)); Text(r.job, fontSize = 11.sp, color = Color(0xFF75695F)); Spacer(Modifier.weight(1f)); Text("🔧 ${r.work}", fontSize = 11.sp) }
                    Spacer(Modifier.height(8.dp)); Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MissionButton("超市", { send(r.name, "废弃超市") }); MissionButton("医院", { send(r.name, "旧医院") }); MissionButton("仓库", { send(r.name, "废弃仓库") })
                    }
                }
            }
        }
    }
}

@Composable fun MissionButton(text: String, click: () -> Unit) { OutlinedButton(click, Modifier.height(35.dp), contentPadding = PaddingValues(horizontal = 9.dp), shape = RoundedCornerShape(9.dp)) { Text(text, fontSize = 11.sp) } }

@Composable
fun ShopScreen(money: Int, flash: String, buy: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 18.dp)) {
        item { SectionTitle("废土商店", "今天有这些稳定物资。真正稀缺的东西，需要你自己派人出去找。") }
        item { Card(Modifier.fillMaxWidth().padding(12.dp), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE6D2B4))) { Text("当前资金：$money 废土币\n${if (flash.isEmpty()) "准备补充社区库存" else flash}", Modifier.padding(16.dp), fontWeight = FontWeight.Bold) } }
        item { ShopItem("🍖", "食物箱", "+12 食物", 80) { buy("food") } }
        item { ShopItem("💧", "净水箱", "+12 水", 70) { buy("water") } }
        item { ShopItem("💊", "医疗包", "+3 药品", 120) { buy("medicine") } }
        item { ShopItem("🧱", "建材包", "+10 建材", 100) { buy("materials") } }
    }
}

@Composable fun ShopItem(icon: String, name: String, effect: String, price: Int, click: () -> Unit) { Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp), shape = RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Text(icon, fontSize = 28.sp); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(name, fontWeight = FontWeight.Bold); Text(effect, fontSize = 11.sp, color = Color(0xFF75695F)) }; Button(click, shape = RoundedCornerShape(10.dp)) { Text("$price") } } } }

@Composable
fun SystemScreen(level: Int, day: Int, money: Int, reputation: Int, security: Int, materials: Int, chapter: Int) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 18.dp)) {
        item { SectionTitle("包租公系统", "从一间破房子开始，把 A-17 建成末世中的理想社区。") }
        item { Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp), shape = RoundedCornerShape(21.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE6D2B4))) { Column(Modifier.padding(19.dp)) { Text("SYSTEM LEVEL", fontSize = 11.sp, color = Brown, fontWeight = FontWeight.Bold); Text("Lv.$level", fontSize = 38.sp, fontWeight = FontWeight.Black, color = Dark); Spacer(Modifier.height(8.dp)); LinearProgressIndicator(progress = { (money.toFloat() / (level * 2200f)).coerceIn(0f, 1f) }, Modifier.fillMaxWidth(), color = Green, trackColor = Color(0xFFD3C0A4)); Spacer(Modifier.height(8.dp)); Text("下一等级：${level * 2200} 废土币", fontSize = 11.sp) } } }
        item { DataCard("📖 主线进度", "第 $chapter 章", when (chapter) { 1 -> "离开过去，在 A-17 扎下根。"; 2 -> "第一批邻居开始加入社区。"; else -> "让更多幸存者相信这里可以成为家。" }) }
        item { DataCard("📊 社区数据", "生存第 $day 天", "声望 $reputation · 安全 $security · 建材 $materials") }
        item { DataCard("🎯 核心玩法", "房源 → 租客 → 收租 → 外出 → 设施", "末世里最值钱的不是房子，而是一个能让人安心睡觉的家。") }
    }
}

@Composable fun DataCard(title: String, line1: String, line2: String) { Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp), shape = RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) { Column(Modifier.padding(16.dp)) { Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp); Spacer(Modifier.height(5.dp)); Text(line1, fontSize = 13.sp); Text(line2, fontSize = 11.sp, color = Color(0xFF75695F)) } } }
@Composable fun EmptyBox(text: String) { Card(Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) { Text(text, Modifier.padding(18.dp), color = Color(0xFF75695F)) } }

fun residentPool(): List<Resident> = listOf(
    Resident("林雪", "医生", 12, 88, 110, "治疗伤员"),
    Resident("王猛", "拾荒者", 72, 65, 90, "外出搜集效率提升"),
    Resident("赵小雨", "农夫", 20, 82, 75, "每天额外获得食物"),
    Resident("陈虎机", "守卫", 86, 55, 130, "提高尸潮防御"),
    Resident("苏雅", "商人", 18, 92, 150, "商店价格更稳定"),
    Resident("周凯", "厨师", 25, 86, 100, "降低居民食物消耗"),
    Resident("韩冰", "工程师", 42, 96, 180, "设施升级效率提升"),
    Resident("顾北", "电工", 38, 93, 145, "降低每日电力消耗"),
    Resident("唐宁", "护士", 15, 91, 125, "提高诊所治疗效果"),
    Resident("老梁", "木匠", 46, 89, 105, "外出更容易找到建材")
)
