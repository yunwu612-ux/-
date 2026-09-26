package com.yunwu.apartmentlord

import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.random.Random

data class Room(val id:Int,val name:String,var built:Boolean=false,var rented:Boolean=false,var tenant:String="")
data class Tenant(val name:String,val job:String,val combat:Int,val work:Int,val rent:Int)

class MainActivity: ComponentActivity(){ override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{Game()}} }

@Composable
fun Game(){
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("apartment_lord_v11", Context.MODE_PRIVATE) }
    var day by rememberSaveable { mutableIntStateOf(prefs.getInt("day", 1)) }
    var money by rememberSaveable { mutableIntStateOf(prefs.getInt("money", 800)) }
    var power by rememberSaveable { mutableIntStateOf(prefs.getInt("power", 20)) }
    var water by rememberSaveable { mutableIntStateOf(prefs.getInt("water", 30)) }
    var food by rememberSaveable { mutableIntStateOf(prefs.getInt("food", 40)) }
    var level by rememberSaveable { mutableIntStateOf(prefs.getInt("level", 1)) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var msg by rememberSaveable { mutableStateOf(prefs.getString("msg", "系统：欢迎来到 A-17 号社区。") ?: "系统：欢迎来到 A-17 号社区。") }
    val rooms = remember {
        mutableStateListOf(
            Room(1,"破旧单间", built=prefs.getBoolean("room1", false), rented=prefs.getString("tenant1", "")!!.isNotEmpty(), tenant=prefs.getString("tenant1", "") ?: ""),
            Room(2,"废弃单间", built=prefs.getBoolean("room2", false), rented=prefs.getString("tenant2", "")!!.isNotEmpty(), tenant=prefs.getString("tenant2", "") ?: ""),
            Room(3,"空置房间", built=prefs.getBoolean("room3", false), rented=prefs.getString("tenant3", "")!!.isNotEmpty(), tenant=prefs.getString("tenant3", "") ?: ""),
            Room(4,"空置房间", built=prefs.getBoolean("room4", false), rented=prefs.getString("tenant4", "")!!.isNotEmpty(), tenant=prefs.getString("tenant4", "") ?: ""),
            Room(5,"高级公寓", built=prefs.getBoolean("room5", false), rented=prefs.getString("tenant5", "")!!.isNotEmpty(), tenant=prefs.getString("tenant5", "") ?: ""),
            Room(6,"高级公寓", built=prefs.getBoolean("room6", false), rented=prefs.getString("tenant6", "")!!.isNotEmpty(), tenant=prefs.getString("tenant6", "") ?: "")
        )
    }
    val allCandidates = remember { listOf(
        Tenant("李铁柱","维修工",31,90,70), Tenant("林雪","医生",12,88,110),
        Tenant("王猛","拾荒者",72,65,90), Tenant("赵小雨","农夫",20,82,75),
        Tenant("陈虎机","守卫",86,55,130), Tenant("苏雅","商人",18,92,150),
        Tenant("周凯","厨师",25,86,100), Tenant("韩冰","工程师",42,96,180)
    ) }
    val tenants = remember {
        mutableStateListOf<Tenant>().apply {
            for (candidate in allCandidates) if (prefs.getBoolean("tenant_${candidate.name}", false)) add(candidate)
        }
    }

    fun persist() {
        prefs.edit().apply {
            putInt("day", day).putInt("money", money).putInt("power", power).putInt("water", water)
                .putInt("food", food).putInt("level", level).putString("msg", msg)
            rooms.forEach { r -> putBoolean("room${r.id}", r.built); putString("tenant${r.id}", r.tenant) }
            allCandidates.forEach { c -> putBoolean("tenant_${c.name}", tenants.any { it.name == c.name }) }
            apply()
        }
    }
    LaunchedEffect(day, money, power, water, food, level, msg, tenants.size, rooms.map { it.built to it.tenant }) { persist() }

    fun nextDay(){
        val income = tenants.sumOf { it.rent }
        money += income - tenants.size * 8
        food = max(0, food - tenants.size * 2)
        water = max(0, water - tenants.size * 2)
        day++
        val event = Random.nextInt(100)
        msg = when {
            food == 0 -> "⚠️ 食物耗尽！租客开始抱怨。"
            water == 0 -> "⚠️ 水源不足！社区满意度下降。"
            event < 12 -> "☣️ 丧尸群在社区外徘徊，守卫暂时挡住了它们。"
            event < 24 -> { money += 80; food += 5; water += 5; "📦 政府补给车经过，你获得了少量物资。" }
            else -> "🌙 平安度过第 $day 天，今天收取了 $income 废土币房租。"
        }
        if (money >= level * 1200 && level < 5) { level++; msg += "\n🎉 系统升级！现在是 Lv.$level。" }
    }
    fun build(id:Int){
        val index = rooms.indexOfFirst { it.id == id }
        if (index < 0) return
        val room = rooms[index]
        if (!room.built && money >= 250) { money -= 250; rooms[index] = room.copy(built=true); msg="🏠 建造完成：${room.name}。" }
        else if (!room.built) msg="💰 建造需要 250 废土币。"
    }
    fun rent(id:Int){
        val index = rooms.indexOfFirst { it.id == id }
        if (index < 0) return
        val room = rooms[index]
        if (room.built && !room.rented && tenants.size < level * 3) {
            val candidate = allCandidates.firstOrNull { c -> tenants.none { it.name == c.name } }
            if (candidate != null) { tenants.add(candidate); rooms[index] = room.copy(rented=true, tenant=candidate.name); msg="👤 ${candidate.name} 租下了这间房，每日租金 +${candidate.rent}。" }
            else msg="暂时没有新的租客。"
        } else msg="请先修建房间，或检查房间是否已出租。"
    }

    MaterialTheme(colorScheme=lightColorScheme(primary=Color(0xFF8B5E34),secondary=Color(0xFF6D8B74),background=Color(0xFFF5EBDD))){
        Scaffold(bottomBar={NavigationBar{
            NavigationBarItem(tab==0,{tab=0},icon={Icon(Icons.Default.Home,null)},label={Text("社区")})
            NavigationBarItem(tab==1,{tab=1},icon={Icon(Icons.Default.People,null)},label={Text("租客")})
            NavigationBarItem(tab==2,{tab=2},icon={Icon(Icons.Default.Settings,null)},label={Text("系统")})
        }}){ p ->
            Column(Modifier.fillMaxSize().background(Color(0xFFF5EBDD)).padding(p)){
                Header(day,money,power,water,food,level)
                when(tab){0->Community(rooms,::build,::rent,msg);1->Tenants(tenants);2->System(level)}
                if(tab==0) Button({nextDay()},Modifier.fillMaxWidth().padding(12.dp),shape=RoundedCornerShape(14.dp)){Icon(Icons.Default.FastForward,null);Spacer(Modifier.width(8.dp));Text("进入下一天")}
            }
        }
    }
}
@Composable fun Header(day:Int,money:Int,power:Int,water:Int,food:Int,level:Int){Card(Modifier.fillMaxWidth().padding(12.dp),colors=CardDefaults.cardColors(containerColor=Color(0xFFEAD7B7))){Column(Modifier.padding(14.dp)){Text("☣️ 末世包租公",fontSize=24.sp,fontWeight=FontWeight.Bold);Text("第 $day 天 · 安全区 A-17",fontSize=13.sp);Spacer(Modifier.height(8.dp));Row(Modifier.fillMaxWidth(),Arrangement.SpaceBetween){Text("💰 $money");Text("⚡ $power");Text("💧 $water");Text("🍖 $food");Text("Lv.$level")}}}}
@Composable
fun ColumnScope.Community(rooms:List<Room>,build:(Int)->Unit,rent:(Int)->Unit,msg:String){Text("🏚️ 我的社区",Modifier.padding(horizontal=16.dp),fontSize=20.sp,fontWeight=FontWeight.Bold);Text(msg,Modifier.padding(16.dp),color=Color(0xFF5D5045));LazyColumn(Modifier.weight(1f).padding(horizontal=12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){items(rooms,key={it.id},contentType={"room"}){r->val i=r.id;Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(14.dp)){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Text(if(r.rented)"🏠" else if(r.built)"🔑" else "🏚️",fontSize=32.sp);Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(r.name,fontWeight=FontWeight.Bold);Text(if(r.rented)"租客：${r.tenant}" else if(r.built)"已修复 · 等待租客" else "废墟 · 修建费用 250",fontSize=13.sp)};if(!r.built)Button({build(i)}){Text("建造")}else if(!r.rented)OutlinedButton({rent(i)}){Text("招租")}else Text("已出租",color=Color(0xFF4F7A56),fontWeight=FontWeight.Bold)}}}}}
@Composable fun Tenants(ts:List<Tenant>){Text("👥 我的租客",Modifier.padding(16.dp),fontSize=22.sp,fontWeight=FontWeight.Bold);if(ts.isEmpty())Text("目前还没有租客。\n先修建房间，然后点击“招租”。",Modifier.padding(20.dp))else LazyColumn(Modifier.fillMaxSize().padding(horizontal=12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){items(ts,key={it.name},contentType={"tenant"}){t->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp)){Text("${t.name} · ${t.job}",fontSize=19.sp,fontWeight=FontWeight.Bold);Text("⚔️ 战斗力：${t.combat}");Text("🔧 工作能力：${t.work}");Text("💰 每日房租：${t.rent}")}}}}}
@Composable fun System(level:Int){Text("⚙️ 包租公系统",Modifier.padding(16.dp),fontSize=22.sp,fontWeight=FontWeight.Bold);Card(Modifier.fillMaxWidth().padding(12.dp)){Column(Modifier.padding(18.dp)){Text("系统等级：Lv.$level",fontSize=24.sp,fontWeight=FontWeight.Bold);Text("当前最大租客：${level*3} 人");Text(if(level>=5)"🎉 系统已达到最高等级！" else "升级条件：资金达到 ${level*1200} 废土币")}};Text("玩法提示\n• 建造房间后招募租客\n• 每天自动收取房租\n• 每天消耗食物和水\n• 社区会随机发生末世事件\n• 收入越高，系统升级越快",Modifier.padding(18.dp),lineHeight=25.sp)}
