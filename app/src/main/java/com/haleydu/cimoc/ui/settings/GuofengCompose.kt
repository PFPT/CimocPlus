package com.haleydu.cimoc.ui.settings

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.haleydu.cimoc.R
import com.haleydu.cimoc.data.PreferenceManager

@Composable
fun GuofengComposeTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dark = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
        Configuration.UI_MODE_NIGHT_YES
    val primary = Color(ContextCompat.getColor(context, R.color.colorPrimaryVermillion))
    val surface = Color(ContextCompat.getColor(context, R.color.colorSurface))
    val background = Color(ContextCompat.getColor(context, R.color.colorBackground))
    val onSurface = Color(ContextCompat.getColor(context, R.color.colorOnSurface))
    val colors = if (dark) {
        darkColors(
            primary = primary,
            secondary = primary,
            background = background,
            surface = surface,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = onSurface,
            onSurface = onSurface
        )
    } else {
        lightColors(
            primary = primary,
            secondary = primary,
            background = background,
            surface = surface,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = onSurface,
            onSurface = onSurface
        )
    }
    MaterialTheme(colors = colors, content = content)
}

@Composable
fun GuofengLargeTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(bottom = 8.dp),
        color = MaterialTheme.colors.onSurface,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.SansSerif,
        letterSpacing = (-0.3).sp
    )
}

@Composable
fun PrefGroup(header: Int? = null, content: @Composable ColumnScope.() -> Unit) {
    if (header != null) {
        Text(
            text = stringResource(header),
            modifier = Modifier.padding(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 8.dp),
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.55f),
            fontSize = 13.sp
        )
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colors.surface)
    ) {
        content()
    }
}

@Composable
fun PrefDivider() {
    val context = LocalContext.current
    Divider(
        modifier = Modifier.padding(start = 16.dp),
        color = Color(ContextCompat.getColor(context, R.color.colorSeparator)),
        thickness = 0.5.dp
    )
}

@Composable
fun ActionPref(title: Int, summary: Int? = null, showDivider: Boolean = true, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(title),
                color = MaterialTheme.colors.onSurface,
                fontSize = 17.sp
            )
            if (summary != null) {
                Text(
                    text = stringResource(summary),
                    modifier = Modifier.padding(top = 2.dp),
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.55f),
                    fontSize = 13.sp
                )
            }
        }
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.35f)
        )
    }
    if (showDivider) PrefDivider()
}

@Composable
fun SwitchPref(
    title: Int,
    key: String,
    def: Boolean,
    preference: PreferenceManager,
    showDivider: Boolean = true
) {
    var checked by remember { mutableStateOf(preference.getBoolean(key, def)) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                checked = !checked
                preference.putBoolean(key, checked)
            }
            .padding(start = 16.dp, top = 8.dp, end = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(title),
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colors.onSurface,
            fontSize = 17.sp
        )
        Switch(
            checked = checked,
            onCheckedChange = {
                checked = it
                preference.putBoolean(key, it)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colors.primary,
                checkedTrackColor = MaterialTheme.colors.primary.copy(alpha = 0.35f),
                uncheckedThumbColor = MaterialTheme.colors.onSurface.copy(alpha = 0.35f),
                uncheckedTrackColor = MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
            )
        )
    }
    if (showDivider) PrefDivider()
}
