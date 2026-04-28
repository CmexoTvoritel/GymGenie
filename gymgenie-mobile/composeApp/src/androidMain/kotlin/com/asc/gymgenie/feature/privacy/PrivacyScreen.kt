package com.asc.gymgenie.feature.privacy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.asc.gymgenie.ui.components.GymGenieButton

private val termsText = """
Условия использования приложения GymGenie

1. Общие положения
1.1. Настоящее Пользовательское соглашение (далее — Соглашение) регулирует отношения между владельцем приложения GymGenie (далее — Администрация) и пользователем сети Интернет (далее — Пользователь).

1.2. Администрация оставляет за собой право в любое время изменять, добавлять или удалять пункты настоящего Соглашения без уведомления Пользователя.

2. Предмет соглашения
2.1. Администрация предоставляет Пользователю доступ к мобильному приложению GymGenie, включая функции составления тренировочных планов с помощью искусственного интеллекта, отслеживания прогресса и взаимодействия с чат-ботом тренером.

3. Права и обязанности сторон
3.1. Пользователь обязуется:
- Предоставлять достоверную информацию при регистрации.
- Не использовать приложение в целях, противоречащих законодательству.
- Не передавать данные своей учётной записи третьим лицам.

3.2. Администрация обязуется:
- Обеспечить работоспособность приложения.
- Защищать персональные данные Пользователя в соответствии с политикой конфиденциальности.

4. Обработка персональных данных
4.1. Используя приложение, Пользователь даёт согласие на обработку своих персональных данных, включая: имя, адрес электронной почты, параметры тренировок.

4.2. Данные обрабатываются исключительно в целях предоставления услуг приложения и улучшения качества обслуживания.

5. Ответственность
5.1. Администрация не несёт ответственности за любые прямые или косвенные убытки, возникшие в результате использования или невозможности использования приложения.

5.2. Рекомендации по тренировкам носят информационный характер и не являются медицинскими назначениями. Перед началом тренировок рекомендуется проконсультироваться с врачом.

6. Заключительные положения
6.1. Настоящее Соглашение вступает в силу с момента использования приложения и действует бессрочно.

6.2. Все споры решаются путём переговоров, а при невозможности достижения согласия — в судебном порядке в соответствии с действующим законодательством.
""".trimIndent()

@Composable
fun PrivacyScreen(
    onAccepted: () -> Unit,
) {
    var isChecked by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 24.dp),
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Условия соглашения",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = termsText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = { isChecked = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                ),
            )
            Text(
                text = "Я соглашаюсь с правилами",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        GymGenieButton(
            text = "Принять",
            onClick = onAccepted,
            enabled = isChecked,
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}
