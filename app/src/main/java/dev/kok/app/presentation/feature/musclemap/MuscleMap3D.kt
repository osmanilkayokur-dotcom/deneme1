package dev.kok.app.presentation.feature.musclemap

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.kok.app.domain.model.MuscleGroup
import dev.kok.app.domain.model.MuscleRole
import dev.kok.app.domain.usecase.MuscleHighlight
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.Scene
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import kotlin.math.pow

/** Uygulama içine gömülecek anatomi modelinin yolu (assets/). */
const val ANATOMY_MODEL_ASSET = "models/male_anatomy.glb"

fun hasAnatomyModel(context: Context): Boolean =
    runCatching { context.assets.list("models")?.contains("male_anatomy.glb") == true }.getOrDefault(false)

/**
 * SceneView (Filament) ile 360° döndürülebilir/yakınlaştırılabilir anatomi modeli.
 * Her render düğümünün adı kas grubunun mesh id'siyle eşleştirilir ve materyal
 * baseColor/emissive değerleriyle parlak (çalışan) ya da mat (dinlenen) yapılır.
 */
@Composable
fun MuscleMap3D(
    highlight: MuscleHighlight,
    muscles: List<MuscleGroup>,
    primary: Color,
    secondary: Color,
    rest: Color,
    modifier: Modifier = Modifier,
) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val modelNode = remember(modelLoader) {
        ModelNode(
            modelLoader.createModelInstance(ANATOMY_MODEL_ASSET),
            false, // autoAnimate
            1.0f, // scaleToUnits
            Float3(0f, 0f, 0f), // centerOrigin
        )
    }

    LaunchedEffect(highlight, muscles, modelNode) {
        val roleByMesh = muscles.associate { it.meshNodeId.lowercase() to highlight.roleOf(it.code) }
        modelNode.renderableNodes.forEach { node ->
            val name = node.name?.lowercase() ?: return@forEach
            // Model düğüm adı mesh id ile başlıyorsa (ör. mesh_biceps_brachii_l) eşleşir.
            val match = roleByMesh.entries.firstOrNull { name.startsWith(it.key) } ?: return@forEach
            val (color, glow) = when (match.value) {
                MuscleRole.PRIMARY -> primary to 0.55f
                MuscleRole.SECONDARY -> secondary to 0.25f
                null -> rest to 0f
            }
            node.materialInstances.forEach { mi ->
                runCatching {
                    mi.setParameter("baseColorFactor", color.red.lin(), color.green.lin(), color.blue.lin(), 1f)
                    mi.setParameter("emissiveFactor", color.red.lin() * glow, color.green.lin() * glow, color.blue.lin() * glow)
                }
            }
        }
    }

    Box(modifier.fillMaxWidth().height(380.dp)) {
        Scene(
            modifier = Modifier.fillMaxWidth().height(380.dp),
            engine = engine,
            modelLoader = modelLoader,
            isOpaque = false,
            childNodes = listOf(modelNode),
            cameraManipulator = rememberCameraManipulator(
                orbitHomePosition = Float3(0f, 0f, 2.2f),
                targetPosition = Float3(0f, 0f, 0f),
            ),
        )
    }
}

/** Filament doğrusal renk uzayı bekler: sRGB → lineer yaklaşık dönüşüm. */
private fun Float.lin(): Float = this.toDouble().pow(2.2).toFloat()
