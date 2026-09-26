package com.yunwu.apartmentlord

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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

data class Room(val id:Int,val name:String,var built:Boolean=false,var rented:Boolean=false,var tenant:String="")
data class Tenant(val name:String,val job:String,val combat:Int,val work:Int,val rent:Int)

private val Sand = Color(0xFFF2E7D5)
private val Dust = Color(0xFFD8C3A5)
private val Brown = Color(0xFF795548)
private val Dark = Color(0xFF2F2924)
private val Green = Color(0xFF657C5A)
private val Red = Color(0xFFB65F52)
private val Blue = Color(0xFF5F8296)

class MainActivity: ComponentActivity(){
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        setContent { Game() }
    }
}

@Composable
fun Game(){
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("apartment_lord_v12", Context.MODE_PRIVATE) }
    var day by rememberSaveable { mutableIntStateOf(prefs.getInt("day",1)) }
    var money by rememberSaveable { mutableIntStateOf(prefs.getInt("money",800)) }
    var power by rememberSaveable { mutableIntStateOf(prefs.getInt("power",20)) }
    var water by rememberSaveable { mutableIntStateOf(prefs.getInt("water",30)) }
    var food by rememberSaveable { mutableIntStateOf(prefs.getInt("food",40)) }
    var level by rememberSaveable { mutableIntStateOf(prefs.getInt("level",1)) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var msg by rememberSaveable { mutableStateOf(prefs.getString("msg","系统：欢迎来到 A-17 号安全社区。") ?: "系统：欢迎来到 A-17 号安全社区。") }

    val rooms = remember {
        mutableStateListOf(*List(9) { index ->
            val id=index+1
            Room(id, if(id<=4) "废墟单间" else if(id<=7) "标准公寓" else "高级公寓",
                prefs.getBoolean("room${id}",false), prefs.getString("tenant$id","")!!.isNotEmpty(), prefs.getString("tenant$id","") ?: "")
        }.toTypedArray())
    }
    val candidates = remember { listOf(
        Tenant("李铁柱","维修工",31,90,70), Tenant("林雪","医生",12,88,110),
        Tenant("王猛","拾荒者",72,65,90), Tenant("赵小雨","农夫",20,82,75),
        Tenant("陈虎机","守卫",86,55,130), Tenant("苏雅","商人",18,92,150),
        Tenant("周凯","厨师",25,86,100), Tenant("韩冰","工程师",42,96,180)
    ) }
    val tenants = remember { mutableStateListOf<Tenant>().apply { candidates.filter { prefs.getBoolean("tenant_${it.name}",false) }.forEach(::add) } }

    fun persist(){
        prefs.edit().apply {
            putInt("day",day).putInt("money",money).putInt("power",power).putInt("water",water).putInt("food",food).putInt("level",level).putString("msg",msg)
            rooms.forEach { putBoolean("room${it.id}",it.built); putString("tenant${it.id}",it.tenant) }
            candidates.forEach { putBoolean("tenant_${it.name}",tenants.any { t -> t.name==it.name }) }
            apply()
        }
    }
    LaunchedEffect(day,money,power,water,food,level,msg,tenants.size,rooms.map { it.built to it.tenant }) { persist() }

    fun nextDay(){
        val income=tenants.sumOf { it.rent }
        money += income - tenants.size*8
        food=max(0,food-tenants.size*2)
        water=max(0,water-tenants.size*2)
        power=max(0,power-1)
        day++
        val event=Random.nextInt(100)
        msg=when{
            food==0 -> "⚠️ 食物耗尽！社区需要尽快补充物资。"
            water==0 -> "⚠️ 水源不足！社区供水系统亮起红灯。"
            event<12 -> "☣️ 丧尸群在社区外徘徊，守卫暂时挡住了它们。"
            event<24 -> { money+=80; food+=5; water+=5; "📦 政府补给车经过，获得少量物资。" }
            event<34 -> { power+=5; "⚡ 维修工修复了备用发电机，电力 +5。" }
            else -> "🌙 平安度过第 $day 天，今天收取了 $income 废土币房租。"
        }
        if(money>=level*1200 && level<5){ level++; msg += "\n🎉 包租公系统升级！现在是 Lv.$level。" }
    }
    fun build(id:Int){
        val index=rooms.indexOfFirst { it.id==id }; if(index<0)return
        val r=rooms[index]
        if(!r.built && money>=250){ money-=250; rooms[index]=r.copy(built=true); msg="🏠 修复完成：${r.name}。现在可以招租了。" }
        else if(!r.built) msg="💰 修复需要 250 废土币。"
    }
    fun rent(id:Int){
        val index=rooms.indexOfFirst { it.id==id }; if(index<0)return
        val r=rooms[index]
        if(r.built && !r.rented && tenants.size<level*3){
            val candidate=candidates.firstOrNull { c -> tenants.none { it.name==c.name } }
            if(candidate!=null){ tenants.add(candidate); rooms[index]=r.copy(rented=true,tenant=candidate.name); msg="👤 ${candidate.name} 入住了社区，每日租金 +${candidate.rent}。" }
            else msg="暂时没有新的租客。"
        } else if(tenants.size>=level*3) msg="🔒 当前系统等级最多容纳 ${level*3} 名租客。"
        else msg="请先修复房间。"
    }

    MaterialTheme(colorScheme=lightColorScheme(primary=Brown,secondary=Green,background=Sand,surface=Color.White)){
        Scaffold(bottomBar={
            NavigationBar(containerColor=Color.White){
                NavigationBarItem(tab==0,{tab=0},icon={Icon(Icons.Default.Home,null)},label={Text("社区")})
                NavigationBarItem(tab==1,{tab=1},icon={Icon(Icons.Default.People,null)},label={Text("租客")})
                NavigationBarItem(tab==2,{tab=2},icon={Icon(Icons.Default.Settings,null)},label={Text("系统")})
            }
        }) { p ->
            Column(Modifier.fillMaxSize().background(Sand).padding(p)){
                GameHeader(day,money,power,water,food,level)
                when(tab){
                    0 -> Community(rooms,::build,::rent,msg)
                    1 -> Tenants(tenants)
                    2 -> SystemPanel(level,day,money)
                }
                if(tab==0){
                    Button({nextDay()},Modifier.fillMaxWidth().padding(12.dp),shape=RoundedCornerShape(16.dp),colors=ButtonDefaults.buttonColors(containerColor=Brown)){
                        Icon(Icons.Default.FastForward,null); Spacer(Modifier.width(8.dp)); Text("进入下一天")
                    }
                }
            }
        }
    }
}

@Composable
fun GameHeader(day:Int,money:Int,power:Int,water:Int,food:Int,level:Int){
    Box(Modifier.fillMaxWidth().height(158.dp).clip(RoundedCornerShape(bottomStart=26.dp,bottomEnd=26.dp)).background(Color(0xFF3D3935))){
        Canvas(Modifier.fillMaxSize()){
            val path=Path().apply { moveTo(0f,size.height*.68f); lineTo(size.width*.25f,size.height*.45f); lineTo(size.width*.48f,size.height*.72f); lineTo(size.width*.72f,size.height*.42f); lineTo(size.width,size.height*.66f); lineTo(size.width,size.height); lineTo(0f,size.height); close() }
            drawPath(path,Color(0xFF514B45))
        }
        Column(Modifier.padding(18.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(Color(0xFFD2A96C)),contentAlignment=Alignment.Center){Icon(Icons.Default.HomeWork,null,tint=Dark)}
                Spacer(Modifier.width(12.dp)); Column{Text("末世包租公",color=Color.White,fontSize=22.sp,fontWeight=FontWeight.Black);Text("A-17 安全社区 · 第 $day 天",color=Color(0xFFE5D8C8),fontSize=12.sp)}
                Spacer(Modifier.weight(1f)); Surface(shape=RoundedCornerShape(50),color=Color(0xFF6B7E5C)){Text("Lv.$level",Modifier.padding(horizontal=12.dp,vertical=7.dp),color=Color.White,fontWeight=FontWeight.Bold)}
            }
            Spacer(Modifier.height(14.dp))
            LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                item{ResourceChip("💰",money.toString())}; item{ResourceChip("⚡",power.toString())}; item{ResourceChip("💧",water.toString())}; item{ResourceChip("🍖",food.toString())}
            }
        }
    }
}

@Composable fun ResourceChip(icon:String,value:String){Surface(shape=RoundedCornerShape(12.dp),color=Color(0xAAFFFFFF)){Row(Modifier.padding(horizontal=9.dp,vertical=5.dp),verticalAlignment=Alignment.CenterVertically){Text(icon,fontSize=14.sp);Spacer(Modifier.width(4.dp));Text(value,color=Dark,fontWeight=FontWeight.Bold,fontSize=12.sp)}}}

@Composable
fun ColumnScope.Community(rooms:List<Room>,build:(Int)->Unit,rent:(Int)->Unit,msg:String){
    Text("社区地图",Modifier.padding(start=16.dp,top=12.dp,end=16.dp),fontSize=20.sp,fontWeight=FontWeight.Bold,color=Dark)
    Text("修复废墟、招募租客，让 A-17 重新活起来。",Modifier.padding(start=16.dp,end=16.dp,top=2.dp,bottom=8.dp),fontSize=12.sp,color=Color(0xFF75695F))
    Surface(Modifier.padding(horizontal=12.dp).fillMaxWidth().height(76.dp),shape=RoundedCornerShape(18.dp),color=Color(0xFFE1D0B7)){
        Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){
            Box(Modifier.size(48.dp).clip(CircleShape).background(Color(0xFF8B6B4B)),contentAlignment=Alignment.Center){Icon(Icons.Default.Campaign,null,tint=Color.White)}
            Spacer(Modifier.width(10.dp)); Text(msg,Modifier.weight(1f),fontSize=12.sp,color=Dark)
        }
    }
    Spacer(Modifier.height(10.dp))
    LazyVerticalGrid(columns=GridCells.Fixed(3),modifier=Modifier.weight(1f).padding(horizontal=12.dp),contentPadding=PaddingValues(bottom=10.dp),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
        items(rooms,key={it.id},contentType={"room"}){ r ->
            RoomTile(r,{build(r.id)},{rent(r.id)})
        }
    }
}

@Composable
fun RoomTile(room:Room,onBuild:()->Unit,onRent:()->Unit){
    val bg=when{room.rented->Color(0xFFDDE8D7);room.built->Color(0xFFE8DDC8);else->Color(0xFFC9B9A5)}
    Card(shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=bg),modifier=Modifier.fillMaxWidth().height(154.dp)){
        Column(Modifier.padding(10.dp),horizontalAlignment=Alignment.CenterHorizontally){
            Box(Modifier.fillMaxWidth().height(62.dp).clip(RoundedCornerShape(12.dp)).background(if(room.built)Color(0xFFB99167) else Color(0xFF9B8976)),contentAlignment=Alignment.Center){
                Icon(if(room.rented)Icons.Default.Person else if(room.built)Icons.Default.Key else Icons.Default.HomeRepairService,null,tint=Color.White,modifier=Modifier.size(34.dp))
            }
            Spacer(Modifier.height(7.dp)); Text("#${room.id} ${room.name}",fontSize=12.sp,fontWeight=FontWeight.Bold,maxLines=1)
            Text(if(room.rented)room.tenant else if(room.built)"等待租客" else "废墟",fontSize=10.sp,color=Color(0xFF65594F))
            Spacer(Modifier.height(4.dp))
            when{!room.built->SmallActionButton("修复",onBuild);!room.rented->SmallActionButton("招租",onRent);else->Text("已出租",fontSize=11.sp,color=Green,fontWeight=FontWeight.Bold)}
        }
    }
}

@Composable fun SmallActionButton(text:String,onClick:()->Unit){Button(onClick,Modifier.height(30.dp).fillMaxWidth(),contentPadding=PaddingValues(0.dp),shape=RoundedCornerShape(9.dp),colors=ButtonDefaults.buttonColors(containerColor=Brown)){Text(text,fontSize=11.sp)}}

@Composable
fun Tenants(ts:List<Tenant>){
    Column(Modifier.fillMaxSize()){
        Text("社区居民",Modifier.padding(16.dp),fontSize=22.sp,fontWeight=FontWeight.Bold,color=Dark)
        if(ts.isEmpty()) Text("还没有居民入住。\n去社区地图修复房间并招租吧。",Modifier.padding(20.dp),color=Color(0xFF75695F))
        else LazyColumn(Modifier.fillMaxSize().padding(horizontal=12.dp),verticalArrangement=Arrangement.spacedBy(9.dp),contentPadding=PaddingValues(bottom=12.dp)){
            items(ts,key={it.name},contentType={"tenant"}){t->TenantCard(t)}
        }
    }
}
@Composable fun TenantCard(t:Tenant){Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=Color.White)){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(52.dp).clip(CircleShape).background(Dust),contentAlignment=Alignment.Center){Icon(Icons.Default.Person,null,tint=Brown,modifier=Modifier.size(30.dp))};Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(t.name,fontSize=18.sp,fontWeight=FontWeight.Bold);Text(t.job,fontSize=12.sp,color=Color(0xFF75695F));Spacer(Modifier.height(6.dp));Text("⚔ ${t.combat}   🔧 ${t.work}   💰 ${t.rent}/天",fontSize=12.sp,color=Dark)}}}}

@Composable fun SystemPanel(level:Int,day:Int,money:Int){Column(Modifier.fillMaxSize().padding(16.dp)){Text("包租公系统",fontSize=24.sp,fontWeight=FontWeight.Bold,color=Dark);Spacer(Modifier.height(12.dp));Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Color(0xFFE6D2B4))){Column(Modifier.padding(20.dp)){Text("SYSTEM LEVEL",fontSize=11.sp,color=Brown,fontWeight=FontWeight.Bold);Text("Lv.$level",fontSize=38.sp,fontWeight=FontWeight.Black,color=Dark);Spacer(Modifier.height(8.dp));LinearProgressIndicator({level/5f},Modifier.fillMaxWidth(),color=Green,trackColor=Color(0xFFD3C0A4));Spacer(Modifier.height(10.dp));Text(if(level>=5)"系统已达到最高等级" else "下一等级资金门槛：${level*1200} 废土币",fontSize=12.sp)}};Spacer(Modifier.height(12.dp));InfoCard("经营数据","生存第 $day 天","当前资金 $money 废土币");Spacer(Modifier.height(10.dp));InfoCard("玩法提示","房间 → 租客 → 房租 → 升级","注意每天的食物、水、电力消耗")}}
@Composable fun InfoCard(title:String,line1:String,line2:String){Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=Color.White)){Column(Modifier.padding(16.dp)){Text(title,fontWeight=FontWeight.Bold,fontSize=16.sp);Spacer(Modifier.height(6.dp));Text(line1,fontSize=13.sp);Text(line2,fontSize=13.sp,color=Color(0xFF75695F))}}}
