package com.thehub.hb.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thehub.hb.ui.components.AppLogo
import com.thehub.hb.ui.theme.HubBlack
import com.thehub.hb.ui.theme.HubBorder
import com.thehub.hb.ui.theme.HubMuted
import com.thehub.hb.ui.theme.HubSecondary
import com.thehub.hb.ui.theme.HubSurfaceDark
import com.thehub.hb.ui.theme.HubSurfaceElevated
import com.thehub.hb.ui.theme.HubWhite

private val GoogleBlue = Color(0xFF4285F4)
private val GoogleRed = Color(0xFFEA4335)
private val GoogleYellow = Color(0xFFFBBC05)
private val GoogleGreen = Color(0xFF34A853)

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToFeed: () -> Unit,
    onNavigateToCompleteProfile: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                LoginNavigationEvent.NavigateToFeed -> onNavigateToFeed()
                LoginNavigationEvent.NavigateToCompleteProfile -> onNavigateToCompleteProfile()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HubSurfaceDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(40.dp)
                .clip(CircleShape)
                .background(HubSurfaceElevated)
                .border(1.dp, HubBorder, CircleShape)
                .testTag("login_back_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Retour",
                tint = HubWhite,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AppLogo(size = 104.dp, animated = false)

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "THE HUB",
                color = HubWhite,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 5.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "TEXTE + IMAGE. C'EST TOUT.",
                color = HubSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "Bienvenue sur The Hub. Connectez-vous pour partager vos images et échanger avec la communauté.",
                color = HubSecondary,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
                    .testTag("login_description")
            )

            Spacer(modifier = Modifier.height(34.dp))

            Button(
                onClick = { viewModel.signInWithGoogle(context) },
                enabled = !uiState.isGoogleLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("login_google_button"),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HubWhite,
                    contentColor = HubBlack,
                    disabledContainerColor = HubWhite.copy(alpha = 0.72f),
                    disabledContentColor = HubBlack
                )
            ) {
                if (uiState.isGoogleLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp,
                        color = HubBlack
                    )
                } else {
                    GoogleGLogo()

                    Spacer(modifier = Modifier.size(10.dp))

                    Text(
                        text = "Continuer avec Google",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (!uiState.generalError.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = uiState.generalError.orEmpty(),
                    color = HubMuted,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .testTag("login_error_message")
                )
            }
        }
    }
}

@Composable
private fun GoogleGLogo(
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.Canvas(
        modifier = modifier
            .size(28.dp)
            .testTag("login_google_logo")
    ) {
        val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.35f
        val strokeWidth = size.minDimension * 0.19f
        val arcRect = Rect(
            left = center.x - radius,
            top = center.y - radius,
            right = center.x + radius,
            bottom = center.y + radius
        )
        val stroke = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Butt,
            join = StrokeJoin.Miter
        )

        drawArc(
            color = GoogleRed,
            startAngle = 225f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(arcRect.left, arcRect.top),
            size = androidx.compose.ui.geometry.Size(arcRect.width, arcRect.height),
            style = stroke
        )
        drawArc(
            color = GoogleBlue,
            startAngle = 315f,
            sweepAngle = 95f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(arcRect.left, arcRect.top),
            size = androidx.compose.ui.geometry.Size(arcRect.width, arcRect.height),
            style = stroke
        )
        drawArc(
            color = GoogleGreen,
            startAngle = 50f,
            sweepAngle = 85f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(arcRect.left, arcRect.top),
            size = androidx.compose.ui.geometry.Size(arcRect.width, arcRect.height),
            style = stroke
        )
        drawArc(
            color = GoogleYellow,
            startAngle = 135f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(arcRect.left, arcRect.top),
            size = androidx.compose.ui.geometry.Size(arcRect.width, arcRect.height),
            style = stroke
        )

        drawLine(
            color = GoogleBlue,
            start = center,
            end = androidx.compose.ui.geometry.Offset(center.x + radius, center.y),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Butt
        )
    }
}
