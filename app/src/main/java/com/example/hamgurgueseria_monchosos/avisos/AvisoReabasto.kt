package com.example.hamgurgueseria_monchosos.avisos

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.hamgurgueseria_monchosos.HamburgueseriaApp
import com.example.hamgurgueseria_monchosos.MainActivity
import com.example.hamgurgueseria_monchosos.R
import com.example.hamgurgueseria_monchosos.data.pronostico.mensajeDeAviso
import io.github.jan.supabase.auth.auth
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Una vez al día (por la mañana) revisa si se acerca un fin de semana o festivo y, si se suele
 * vender más o no alcanzan los insumos, manda una notificación con lo que conviene comprar.
 */
class AvisoReabastoWorker(contexto: Context, params: WorkerParameters) : CoroutineWorker(contexto, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as HamburgueseriaApp
        // Sin sesión de Google no hay datos que consultar (RLS); se intenta mañana.
        app.supabase.auth.awaitInitialization()
        if (app.supabase.auth.currentSessionOrNull() == null) return Result.success()

        val pronostico = runCatching { app.pronostico.consultar() }.getOrElse { return Result.retry() }
        val evento = pronostico.evento ?: return Result.success()
        val (titulo, cuerpo) = mensajeDeAviso(evento, pronostico.porComprar) ?: return Result.success()

        // Un aviso por evento; se repite solo si cambia la lista de faltantes.
        val clave = "${evento.hasta}:${pronostico.porComprar.size}"
        val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getString(ULTIMO_AVISO, null) == clave) return Result.success()

        if (notificar(applicationContext, titulo, cuerpo)) prefs.edit { putString(ULTIMO_AVISO, clave) }
        return Result.success()
    }

    companion object {
        private const val TRABAJO = "aviso-reabasto"
        private const val PREFS = "avisos"
        private const val ULTIMO_AVISO = "ultimo_aviso_reabasto"
        private const val HORA = 10   // 10:00, antes de ir a comprar

        /** Programa la revisión diaria. KEEP: no se reinicia el reloj cada vez que abre la app. */
        fun programar(contexto: Context) {
            val solicitud = PeriodicWorkRequestBuilder<AvisoReabastoWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(msHastaLaProxima(HORA), TimeUnit.MILLISECONDS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(contexto)
                .enqueueUniquePeriodicWork(TRABAJO, ExistingPeriodicWorkPolicy.KEEP, solicitud)
        }

        private fun msHastaLaProxima(hora: Int): Long {
            val ahora = Calendar.getInstance()
            val siguiente = (ahora.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, hora)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (!after(ahora)) add(Calendar.DAY_OF_MONTH, 1)
            }
            return siguiente.timeInMillis - ahora.timeInMillis
        }
    }
}

private const val CANAL = "reabasto"
private const val ID_NOTIFICACION = 1001

/** Muestra la notificación. Devuelve false si el usuario no dio permiso de notificaciones. */
fun notificar(contexto: Context, titulo: String, cuerpo: String): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) return false

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        contexto.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CANAL, "Reabasto de insumos", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Avisos antes de fines de semana y festivos"
            },
        )
    }
    val abrirApp = PendingIntent.getActivity(
        contexto, 0,
        Intent(contexto, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
    val notificacion = NotificationCompat.Builder(contexto, CANAL)
        .setSmallIcon(R.drawable.ic_notificacion)
        .setContentTitle(titulo)
        .setContentText(cuerpo)
        .setStyle(NotificationCompat.BigTextStyle().bigText(cuerpo))
        .setContentIntent(abrirApp)
        .setAutoCancel(true)
        .build()
    NotificationManagerCompat.from(contexto).notify(ID_NOTIFICACION, notificacion)
    return true
}
