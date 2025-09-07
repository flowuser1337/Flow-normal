package dev.lvstrng.argon.module.modules.client;

import dev.lvstrng.argon.event.events.HudListener;
import dev.lvstrng.argon.event.events.MouseMoveListener;
import dev.lvstrng.argon.module.Category;
import dev.lvstrng.argon.module.Module;
import dev.lvstrng.argon.module.setting.BooleanSetting;
import dev.lvstrng.argon.module.setting.MinMaxSetting;
import dev.lvstrng.argon.module.setting.NumberSetting;
import dev.lvstrng.argon.utils.*;
import dev.lvstrng.argon.utils.rotation.Rotation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.SwordItem;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public final class AimAssist extends Module implements HudListener, MouseMoveListener {
    // --- SETTINGS ---
    private final BooleanSetting stickyAim = new BooleanSetting("Sticky Aim", false)
            .setDescription("Aims at the last attacked player");

    private final BooleanSetting onlyWeapon = new BooleanSetting("Only Weapon", true);

    private final BooleanSetting onLeftClick = new BooleanSetting("On Left Click", false)
            .setDescription("Only gets triggered if holding down left click");

    private final NumberSetting radius = new NumberSetting("Radius", 0.1, 8, 5, 0.1);

    private final BooleanSetting seeOnly = new BooleanSetting("See Only", true);

    private final NumberSetting fov = new NumberSetting("FOV", 5, 360, 180, 1);
    
    // --- NUEVOS CONTROLES PARA VELOCIDAD Y SUAVIDAD ---
    private final MinMaxSetting speed;
    private final MinMaxSetting smoothing;
    private final NumberSetting prediction;

    private final BooleanSetting yawAssist = new BooleanSetting("Horizontal", true);
    private final BooleanSetting pitchAssist = new BooleanSetting("Vertical", true);

    private final TimerUtils mouseMoveTimer = new TimerUtils();
    private boolean hasUserMovedMouse;

    // --- VARIABLES MEJORADAS PARA STICKY AIM ---
    private PlayerEntity lockedTarget = null;
    private final TimerUtils stickyTimer = new TimerUtils();

    private PlayerEntity currentTarget = null;
    private Vec3d smoothedTargetPos = null;
    private Vec3d targetVelocity = Vec3d.ZERO; // Almacena la velocidad del objetivo

    public AimAssist() {
        super("Aim Assist",
                "Automatically aims at players for you",
                -1,
                Category.FLOW);
        
        speed = new MinMaxSetting("Velocidad", 1, 20, 0.1, 7, 10);
        speed.setDescription("Controla la velocidad de la asistencia. Más alto es más rápido.");
        
        smoothing = new MinMaxSetting("Suavizado", 1, 15, 0.1, 2, 4);
        smoothing.setDescription("Controla la suavidad del movimiento. Más bajo es más suave.");

        prediction = new NumberSetting("Predicción", 0, 5, 1.2, 0.1)
                .setDescription("Anticipa el movimiento del enemigo. 0 = desactivado.");

        addSettings(stickyAim, onlyWeapon, onLeftClick, radius, seeOnly, fov, speed, smoothing, prediction, yawAssist, pitchAssist);
    }
    
    /*
     * IMPORTANTE: Debes conectar este método a tu sistema de eventos.
     * Este método debe ser llamado cada vez que el jugador ataque a una entidad.
     */
    public void onAttack(Entity target) {
        if (stickyAim.getValue() && target instanceof PlayerEntity) {
            // Al atacar a un jugador, lo fijamos como objetivo y reiniciamos el timer.
            lockedTarget = (PlayerEntity) target;
            stickyTimer.reset();
        }
    }

    @Override
    public void onEnable() {
        hasUserMovedMouse = false;
        mouseMoveTimer.reset();
        currentTarget = null;
        lockedTarget = null;
        smoothedTargetPos = null;
        targetVelocity = Vec3d.ZERO;
        eventManager.add(HudListener.class, this);
        eventManager.add(MouseMoveListener.class, this);
        // Aquí deberías registrar tu listener de eventos de ataque si es necesario
        super.onEnable();
    }

    @Override
    public void onDisable() {
        eventManager.remove(HudListener.class, this);
        eventManager.remove(MouseMoveListener.class, this);
        // Aquí deberías desregistrar tu listener de eventos de ataque
        super.onDisable();
    }

    @Override
    public void onRenderHud(HudEvent event) {
        if (mc.player == null || mc.currentScreen != null)
            return;

        float delta = (float) RenderUtils.deltaTime();
        if (delta == 0) delta = 1f / 240f;

        if (onLeftClick.getValue() && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS)
            return;

        if (onlyWeapon.getValue() && !(mc.player.getMainHandStack().getItem() instanceof SwordItem || mc.player.getMainHandStack().getItem() instanceof AxeItem))
            return;

        if (hasUserMovedMouse && mouseMoveTimer.delay(80)) {
            hasUserMovedMouse = false;
        }
        if (hasUserMovedMouse) {
            return;
        }

        // --- LÓGICA DE TARGETING MEJORADA PARA STICKY AIM ---
        PlayerEntity target = null;
        final int STICKY_DURATION_MS = 2500; // Duración fija como solicitaste

        // 1. Comprueba si hay un objetivo fijado y si todavía es válido.
        if (stickyAim.getValue() && lockedTarget != null && !stickyTimer.delay(STICKY_DURATION_MS) && lockedTarget.isAlive() && mc.player.distanceTo(lockedTarget) < radius.getValue()) {
            // Si es válido, ese es nuestro objetivo.
            target = lockedTarget;
        } else {
            // 2. Si no, busca al jugador más cercano como antes.
            lockedTarget = null; // Limpia el objetivo fijado.
            target = WorldUtils.findNearestPlayer(mc.player, radius.getValueFloat(), seeOnly.getValue(), false);
        }
        // --- FIN DE LA LÓGICA DE TARGETING ---


        if (target == null || !(target instanceof PlayerEntity)) { // Explicitly check if the target is a player
            currentTarget = null;
            targetVelocity = Vec3d.ZERO; // Resetea la velocidad si no hay objetivo
            return;
        }

        // --- LÓGICA DE PREDICCIÓN DE MOVIMIENTO ---
        if (currentTarget != target) {
            currentTarget = target;
            smoothedTargetPos = getClosestPointOnHitbox(target);
            targetVelocity = Vec3d.ZERO; // Resetea la velocidad al cambiar de objetivo
        } else {
            // Calcula la velocidad actual del objetivo
            Vec3d currentPos = target.getPos();
            Vec3d prevPos = new Vec3d(target.prevX, target.prevY, target.prevZ);
            targetVelocity = currentPos.subtract(prevPos).multiply(1.0 / delta);
        }

        // Calcula la posición futura del objetivo
        float predictionFactor = prediction.getValueFloat() * delta;
        Vec3d predictedPos = getClosestPointOnHitbox(target).add(targetVelocity.multiply(predictionFactor));
        
        // Suaviza el seguimiento hacia la posición predicha
        float targetSmoothingValue = 15.0f - smoothing.getRandomValueFloat(); // El suavizado también afecta el seguimiento
        double interpFactor = 1.0 - Math.exp(-targetSmoothingValue * delta);
        smoothedTargetPos = smoothedTargetPos.lerp(predictedPos, interpFactor);

        Rotation targetRotation = RotationUtils.getDirection(mc.player, smoothedTargetPos);

        double angleToTarget = RotationUtils.getAngleToRotation(targetRotation);
        if (angleToTarget > (double) fov.getValueInt() / 2)
            return;

        if (WorldUtils.getHitResult(radius.getValue()) instanceof EntityHitResult hitResult && hitResult.getEntity() == target) {
            return;
        }

        float yawError = (float) MathHelper.wrapDegrees(targetRotation.yaw() - mc.player.getYaw());
        float pitchError = (float) (targetRotation.pitch() - mc.player.getPitch());

        // Combina velocidad y suavizado para el movimiento final
        float speedValue = speed.getRandomValueFloat();
        float interp = (float) (1.0 - Math.exp(-speedValue * delta));
        
        float yawStep = yawError * interp;
        float pitchStep = pitchError * interp;

        float newYaw = mc.player.getYaw() + yawStep;
        float newPitch = mc.player.getPitch() + pitchStep;

        newPitch = MathHelper.clamp(newPitch, -90.0f, 90.0f);

        if (yawAssist.getValue()) {
            mc.player.setYaw(newYaw);
        }

        if (pitchAssist.getValue()) {
            mc.player.setPitch(newPitch);
        }
    }
    
    private Vec3d getClosestPointOnHitbox(PlayerEntity target) {
        Vec3d eyePos = mc.player.getEyePos();
        var box = target.getBoundingBox();
        double x = MathHelper.clamp(eyePos.x, box.minX, box.maxX);
        double y = MathHelper.clamp(eyePos.y, box.minY, box.maxY);
        double z = MathHelper.clamp(eyePos.z, box.minZ, box.maxZ);
        return new Vec3d(x, y, z);
    }

    @Override
    public void onMouseMove(MouseMoveEvent event) {
        hasUserMovedMouse = true;
        mouseMoveTimer.reset();
    }
}