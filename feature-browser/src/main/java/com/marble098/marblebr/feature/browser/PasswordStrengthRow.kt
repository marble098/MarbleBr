package com.marble098.marblebr.feature.browser

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.marble098.marblebr.core.i18n.MarbleStrings
import com.marble098.marblebr.core.model.PasswordStrength
import com.marble098.marblebr.core.model.PasswordStrengthLevel

/**
 * قدرتِ گذرواژه، بدونِ نمایشِ خودِ گذرواژه.
 *
 * چرا نوارِ گرادیانی نیست؟ چون رنگِ یکنوا در هر مرحله با متنِ همان مرحله همرنگ است و برای
 * کوررنگی هم قابلِ تشخیص می‌ماند؛ عددِ درصد هم کنارش نوشته می‌شود.
 */
@Composable
internal fun PasswordStrengthRow(
    strength: PasswordStrength,
    strings: MarbleStrings,
    modifier: Modifier = Modifier,
) {
    // برچسب از همان نگاشتِ مشترکِ صفحه می‌آید تا دو جای مختلف دو متنِ متفاوت برای یک سطح ندهند.
    val label = strengthLabel(strength.level, strings)
    val track = when (strength.level) {
        PasswordStrengthLevel.VERY_WEAK, PasswordStrengthLevel.WEAK -> MaterialTheme.colorScheme.error
        PasswordStrengthLevel.FAIR -> MaterialTheme.colorScheme.tertiary
        PasswordStrengthLevel.STRONG, PasswordStrengthLevel.EXCELLENT -> MaterialTheme.colorScheme.primary
    }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = strings.passwordStrengthLabel(label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${strength.score}%",
                style = MaterialTheme.typography.labelMedium,
                color = track,
            )
        }
        LinearProgressIndicator(
            progress = { strength.fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = track,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round,
        )
    }
}
