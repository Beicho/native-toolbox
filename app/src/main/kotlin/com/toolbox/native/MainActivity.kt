package com.toolbox.native

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.toolbox.native.ui.theme.NativeToolboxTheme
import com.toolbox.native.ui.encoding.EncodingScreen
import com.toolbox.native.ui.novel.NovelScreen
import com.toolbox.native.ui.about.AboutScreen
import com.google.accompanist.pager.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NativeToolboxTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun MainScreen() {
    val pagerState = rememberPagerState()
    val tabs = listOf("编码转换", "小说下载", "关于")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("综合工具包") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        text = { Text(title) },
                        selected = pagerState.currentPage == index,
                        onClick = {
                            // 点击切换页面
                        }
                    )
                }
            }

            HorizontalPager(
                count = tabs.size,
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> EncodingScreen()
                    1 -> NovelScreen()
                    2 -> AboutScreen()
                }
            }
        }
    }
}
