package com.example.ieeeconnect.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicText

@Composable
fun HomeScreen(headlines: List<String>) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Header()
            headlines.forEach { title ->
                HeadlineCard(title)
            }
        }
    }
}

@Composable
private fun Header() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BasicText(
            text = "IEEE Connect",
            style = androidx.compose.ui.text.TextStyle(
                color = Color(0xFFE2E8F0),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        )
        BasicText(
            text = "What's happening",
            style = androidx.compose.ui.text.TextStyle(
                color = Color(0xFF94A3B8),
                fontSize = 15.sp
            )
        )
    }
}

@Composable
private fun HeadlineCard(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E293B), RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BasicText(
                text = title,
                style = androidx.compose.ui.text.TextStyle(
                    color = Color(0xFFE2E8F0),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
            BasicText(
                text = "Stay tuned for details and join the conversation.",
                style = androidx.compose.ui.text.TextStyle(
                    color = Color(0xFFCBD5E1),
                    fontSize = 14.sp
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .size(width = 120.dp, height = 36.dp)
                    .background(Color(0xFF38BDF8), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    text = "View",
                    style = androidx.compose.ui.text.TextStyle(
                        color = Color(0xFF0B1021),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    HomeScreen(headlines = listOf(
        "Welcome to IEEE Connect",
        "Upcoming AGM on Friday",
        "Check out new event rooms"
    ))
}
