package com.tindaflow.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.layout.ContentScale
import com.tindaflow.app.R
import com.tindaflow.app.data.*
import com.tindaflow.app.ui.TindaColors

@Composable
fun BrandMark(modifier: Modifier = Modifier) {
    Canvas(modifier.size(31.dp)) {
        val width = size.width; val height = size.height
        val leaf1 = Path().apply {
            moveTo(width * .50f, height * .84f)
            cubicTo(width * .18f, height * .82f, width * .03f, height * .56f, width * .11f, height * .13f)
            cubicTo(width * .35f, height * .20f, width * .57f, height * .33f, width * .50f, height * .84f)
            close()
        }
        val leaf2 = Path().apply {
            moveTo(width * .50f, height * .84f)
            cubicTo(width * .38f, height * .33f, width * .60f, height * .04f, width * .95f, height * .08f)
            cubicTo(width * .95f, height * .47f, width * .73f, height * .78f, width * .50f, height * .84f)
            close()
        }
        drawPath(leaf1, Color(0xFF0F7052))
        drawPath(leaf2, Brush.linearGradient(listOf(Color(0xFF278A60), Color(0xFF04452F))))
    }
}

@Composable
fun WorkspaceHeader(store: DemoStore, onToast: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BrandMark()
        Text("TindaFlow", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TindaColors.Deep, letterSpacing = (-.6).sp)
        Spacer(Modifier.weight(1f))
        Box {
            Row(
                modifier = Modifier.clip(RoundedCornerShape(15.dp))
                    .background(Color.White).border(1.dp, TindaColors.Border, RoundedCornerShape(15.dp))
                    .clickable { expanded = true }.padding(horizontal = 9.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(25.dp).clip(RoundedCornerShape(8.dp)).background(TindaColors.LightGreen), contentAlignment = Alignment.Center) {
                    Text(store.active.initials.take(1), color = TindaColors.Forest, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Spacer(Modifier.width(6.dp))
                Text(store.active.name, modifier = Modifier.widthIn(max = 102.dp), fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Icon(Icons.Default.KeyboardArrowDown, null, modifier = Modifier.size(17.dp))
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                store.workspaces.forEachIndexed { index, business ->
                    DropdownMenuItem(
                        text = { Column {
                            Text(business.name, fontWeight = FontWeight.SemiBold)
                            Text(business.kind, fontSize = 11.sp, color = TindaColors.Muted)
                        } },
                        onClick = {
                            store.switchBusiness(index)
                            expanded = false
                            onToast("Switched to ${business.name} · demo")
                        },
                        leadingIcon = { Icon(Icons.Default.Store, null) }
                    )
                }
            }
        }
        IconButton(onClick = { onToast("Notifications arrive in the cloud milestone") }, modifier = Modifier.size(35.dp)) {
            Icon(Icons.Default.NotificationsNone, contentDescription = stringResource(R.string.notifications), tint = TindaColors.Ink, modifier = Modifier.size(23.dp))
        }
    }
}

@Composable
fun SurfacePanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = modifier.shadow(3.dp, RoundedCornerShape(19.dp), ambientColor = Color(0x0A000000))
        .clip(RoundedCornerShape(19.dp)).background(Color.White).border(1.dp, TindaColors.Border, RoundedCornerShape(19.dp))
        .padding(14.dp), content = content)
}

@Composable
fun SectionTitle(title: String, action: String? = null, onClick: (() -> Unit)? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (action != null && onClick != null) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(onClick = onClick).padding(6.dp)) {
                Text(action, fontSize = 12.sp, color = TindaColors.Muted)
                Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(18.dp), tint = TindaColors.Muted)
            }
        }
    }
}

@Composable
fun PageTitle(title: String, action: String? = null, onAction: (() -> Unit)? = null, greeting: Boolean = false) {
    Column {
        if (greeting) Text(stringResource(R.string.good_morning), color = TindaColors.Muted, fontSize = 14.sp)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.headlineLarge)
            if (action != null && onAction != null) {
                Button(onClick = onAction, shape = RoundedCornerShape(15.dp), contentPadding = PaddingValues(horizontal = 13.dp, vertical = 2.dp)) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(action, fontSize = 12.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun SearchBox(text: String, onText: (String) -> Unit, hint: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp))
        .background(Color.White).border(1.dp, TindaColors.Border, RoundedCornerShape(17.dp))
        .height(52.dp).padding(horizontal = 15.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Search, contentDescription = null, tint = TindaColors.Muted, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = text, onValueChange = onText, singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = TindaColors.Ink),
            modifier = Modifier.weight(1f), decorationBox = { inner ->
                Box {
                    if (text.isEmpty()) Text(hint, fontSize = 13.sp, color = TindaColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    inner()
                }
            }
        )
        if (text.isNotBlank()) IconButton(onClick = { onText("") }, modifier = Modifier.size(29.dp)) {
            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear), modifier = Modifier.size(16.dp), tint = TindaColors.Muted)
        }
    }
}

@Composable
fun ChoiceChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(100.dp))
        .background(if (selected) TindaColors.Forest else Color.White)
        .border(1.dp, if (selected) TindaColors.Forest else TindaColors.Border, RoundedCornerShape(100.dp))
        .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
        Text(text, fontSize = 12.sp, maxLines = 1, color = if (selected) Color.White else TindaColors.Ink, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
fun HeroMetric(label: String, value: String, secondary: String, modifier: Modifier = Modifier,
               chart: Boolean = true) {
    Box(modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
        .background(Brush.linearGradient(listOf(TindaColors.Forest, TindaColors.Deep))).height(164.dp)) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(Color.White.copy(alpha = .025f), radius = size.height * .85f, center = Offset(size.width * .94f, size.height * .2f))
            drawCircle(Color.White.copy(alpha = .025f), radius = size.height * .55f, center = Offset(size.width * .86f, size.height * .02f))
        }
        Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White.copy(alpha = .85f), fontSize = 14.sp)
            Text(value, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, letterSpacing = (-1).sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TrendingUp, null, tint = TindaColors.Lime, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(5.dp))
                Text(secondary, color = TindaColors.Lime, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        if (chart) Sparkline(modifier = Modifier.align(Alignment.BottomEnd).padding(end = 14.dp, bottom = 15.dp).width(115.dp).height(52.dp))
    }
}

@Composable
fun Sparkline(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val heights = listOf(.32f, .41f, .35f, .49f, .65f, .51f, .70f, .61f, .77f, .86f)
        val path = Path()
        heights.forEachIndexed { i, v ->
            val x = (i.toFloat() / (heights.size - 1)) * size.width
            val y = size.height * (1f - v)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, TindaColors.Lime, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(TindaColors.Lime, radius = 4.dp.toPx(), center = Offset(size.width, size.height * (1f - heights.last())))
    }
}

@Composable
fun StatusPill(text: String, kind: String = "green") {
    val (bg, fg) = when (kind) {
        "amber" -> TindaColors.LightAmber to TindaColors.Amber
        "red" -> TindaColors.LightRed to TindaColors.Red
        "gray" -> Color(0xFFF0F2F0) to TindaColors.Ink
        else -> TindaColors.LightGreen to TindaColors.Forest
    }
    Box(Modifier.clip(CircleShape).background(bg).padding(horizontal = 10.dp, vertical = 5.dp)) {
        Text(text, color = fg, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
fun ProductPhoto(art: ProductArt, modifier: Modifier = Modifier) {
    val res = when (art) {
        ProductArt.FABRIC -> R.drawable.product_fabric
        ProductArt.FOAM -> R.drawable.product_foam
        ProductArt.STAPLES -> R.drawable.product_staples
        ProductArt.GLUE -> R.drawable.product_glue
        ProductArt.WOOD -> R.drawable.product_wood
        ProductArt.PACKAGE -> null
    }
    if (res != null) {
        Image(painterResource(res), contentDescription = null, modifier = modifier.clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
    } else {
        Box(modifier.clip(RoundedCornerShape(12.dp)).background(TindaColors.LightGreen), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Inventory2, contentDescription = null, tint = TindaColors.Forest, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
fun LittleIcon(icon: ImageVector, color: Color = TindaColors.Forest, modifier: Modifier = Modifier) {
    Box(modifier.size(43.dp).clip(RoundedCornerShape(13.dp)).background(color.copy(alpha = .10f)), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
    }
}

@Composable
fun LineSeparator() = HorizontalDivider(color = TindaColors.Border, thickness = 1.dp)
