package com.toolbox.nativetoolbox

import android.app.Application
import com.toolbox.nativetoolbox.data.predict.PredictEngine
import com.toolbox.nativetoolbox.data.store.AstroStore
import com.toolbox.nativetoolbox.data.store.LegacyMigration
import com.toolbox.nativetoolbox.net.AstroApi
import com.toolbox.nativetoolbox.util.AssetProvisioner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ToolboxApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AstroApi.init(this)
        AstroStore.init(this)
        AssetProvisioner.init(this)
        PredictEngine.init(this)

        // 旧数据迁移 + 预测事件表清理:放后台,不拖慢启动
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            LegacyMigration.runIfNeeded(this@ToolboxApplication)
            PredictEngine.pruneOldEvents()
        }
    }
}
