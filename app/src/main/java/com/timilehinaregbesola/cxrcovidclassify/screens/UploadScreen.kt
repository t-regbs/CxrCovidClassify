package com.timilehinaregbesola.cxrcovidclassify.screens

import android.Manifest
import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.navigation.NavHostController
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.rememberPagerState
import com.timilehinaregbesola.cxrcovidclassify.R
import com.timilehinaregbesola.cxrcovidclassify.UploadState
import com.timilehinaregbesola.cxrcovidclassify.UploadViewModel
import com.timilehinaregbesola.cxrcovidclassify.components.CovButton
import com.timilehinaregbesola.cxrcovidclassify.components.CovidTopAppBar
import com.timilehinaregbesola.cxrcovidclassify.components.ImageCarousel
import com.timilehinaregbesola.cxrcovidclassify.ml.Covid
import com.timilehinaregbesola.cxrcovidclassify.ui.ashColor
import com.timilehinaregbesola.cxrcovidclassify.ui.homeTextColor
import com.timilehinaregbesola.cxrcovidclassify.ui.marrColor
import com.timilehinaregbesola.cxrcovidclassify.utils.PickImages
import timber.log.Timber

@OptIn(ExperimentalPagerApi::class)
@Composable
fun UploadScreen(navController: NavHostController, viewModel: UploadViewModel) {
    val context = LocalContext.current as Activity
    val state = viewModel.uploadState.observeAsState(UploadState.Init)
    Surface(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Scaffold(
            topBar = {
                CovidTopAppBar(
                    title = "Scan",
                    showBack = true,
                    onBackPressed = {
                        viewModel.setState(UploadState.Init)
                        navController.popBackStack()
                    }
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 24.dp)
                    .scrollable(state = rememberScrollState(), orientation = Orientation.Vertical),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val result = remember { mutableStateOf<List<Uri?>?>(null) }
                var names = mutableListOf<String?>()
                val launcher = rememberLauncherForActivityResult(PickImages()) {
                    result.value = it
                    viewModel.setState(UploadState.Unscanned)
                }
                result.value?.let {
                    pickImage(context, it, launcher)
                    names = getNameList(it, context.contentResolver) as MutableList<String?>
                }
                val pagerState = result.value?.size?.let { it1 ->
                    rememberPagerState(
                        pageCount = it1,
                        initialOffscreenLimit = 2,
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = if (result.value != null) "${pagerState!!.currentPage + 1} of ${result.value!!.size} " else "",
                        lineHeight = 35.sp,
                        textAlign = TextAlign.Center,
                        fontSize = 17.sp,
                        color = homeTextColor
                    )
                    Text(
                        modifier = Modifier.padding(start = 2.dp),
                        text = if (result.value != null) names[pagerState!!.currentPage]!! else "",
                        maxLines = 1,
                        lineHeight = 35.sp,
                        textAlign = TextAlign.Center,
                        fontSize = 17.sp,
                        color = homeTextColor
                    )
                }
                Spacer(modifier = Modifier.height(25.dp))
                if (state.value is UploadState.Init) {
                    Card(
                        modifier = Modifier
                            .width(261.dp)
                            .height(316.dp),
                        shape = MaterialTheme.shapes.medium.copy(CornerSize(24.dp)),
                        elevation = 8.dp
                    ) {
                        Image(painter = painterResource(id = R.drawable.img_p), contentDescription = null)
                    }
                } else {
                    if (pagerState != null) {
                        ImageCarousel(pagerState, result, context)
                    }
                }
                if (state.value is UploadState.Scanned) {
                    Spacer(modifier = Modifier.height(45.dp))
                    if (pagerState != null) {
                        Text(
                            text = "${state.value.pred[pagerState.currentPage].label}: ${state.value.pred[pagerState.currentPage].probabilityString}",
                            lineHeight = 23.87.sp,
                            textAlign = TextAlign.Center,
                            fontSize = 20.sp,
                            color = Color.Black
                        )
                    }
                    Spacer(modifier = Modifier.height(55.dp))
                    CovButton(
                        title = "Re-scan",
                        color = ashColor,
                        onClick = {
                            viewModel.setState(UploadState.Unscanned)
                        },
                        textColor = marrColor
                    )
                } else {
                    Box {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Spacer(modifier = Modifier.height(65.dp))
                            Text(
                                modifier = Modifier
                                    .clickable {
                                        try {
                                            launcher.launch("image/*")
                                        } catch (e: Exception) {
                                            Timber.e(e)
                                        }
                                    },
                                text = if (state.value is UploadState.Init) "Upload" else "Re-Upload",
                                lineHeight = 23.87.sp,
                                textAlign = TextAlign.Center,
                                fontSize = 20.sp,
                                color = marrColor
                            )
                            Spacer(modifier = Modifier.height(35.dp))
                            CovButton(
                                title = "Run scanner",
                                onClick = {
                                    val cxrModel = Covid.newInstance(context)
                                    val bitmaps = mutableListOf<Bitmap>()
                                    for (uri in result.value!!) {
                                        bitmaps.add(uriToBitmap(uri!!, context))
                                    }
                                    viewModel.process(cxrModel, bitmaps)
                                },
                                color = marrColor,
                                enabled = !(state.value is UploadState.Init || state.value is UploadState.Loading)
                            )
                        }
                        if (state.value is UploadState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center),
                                color = marrColor
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun pickImage(
    ctx: Activity,
    uri: List<Uri?>,
    launcher: ManagedActivityResultLauncher<String, List<Uri>>
) {
    if (ActivityCompat.checkSelfPermission(
            ctx,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    ) {
//        launcher.launch(null)
    } else {
        ActivityCompat.requestPermissions(
            ctx,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            1001
        )
    }
}

fun uriToBitmap(uri: Uri, context: Context): Bitmap {
    return MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
}

private fun getNameList(imgs: List<Uri?>, resolver: ContentResolver): List<String?> {
    val list = mutableListOf<String?>()
    for (item in imgs) {
        list.add(queryName(resolver, item))
    }
    return list
}
private fun queryName(resolver: ContentResolver, uri: Uri?): String? {
    val returnCursor: Cursor = resolver.query(uri!!, null, null, null, null)!!
    val nameIndex: Int = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
    returnCursor.moveToFirst()
    val name: String = returnCursor.getString(nameIndex)
    returnCursor.close()
    return name
}
