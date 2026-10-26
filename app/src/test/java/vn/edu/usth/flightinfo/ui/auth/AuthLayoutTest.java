package vn.edu.usth.flightinfo.ui.auth;

import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;

import androidx.core.widget.NestedScrollView;
import android.view.View;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.io.File;
import java.io.FileOutputStream;

import vn.edu.usth.flightinfo.R;

/** Android Views rendered by Robolectric/Skia for layout inspection, not emulator screenshots. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = android.app.Application.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class AuthLayoutTest {
    @Test public void renderSmallPhonePortraitAndLandscapeWithLargeText() throws Exception {
        RuntimeEnvironment.setQualifiers("en-w360dp-h800dp-mdpi");
        try (ActivityController<LoginActivity> controller = Robolectric.buildActivity(LoginActivity.class).setup()) {
            render(controller.get(), 360, 800, "login-en");
        }
        RuntimeEnvironment.setQualifiers("vi-w360dp-h800dp-mdpi");
        try (ActivityController<RegisterActivity> controller = Robolectric.buildActivity(RegisterActivity.class).setup()) {
            render(controller.get(), 360, 800, "register-vi");
        }
        RuntimeEnvironment.setQualifiers("vi-w800dp-h360dp-land-mdpi");
        RuntimeEnvironment.setFontScale(1.5f);
        try (ActivityController<LoginActivity> controller = Robolectric.buildActivity(LoginActivity.class).setup()) {
            render(controller.get(), 800, 360, "login-landscape-large-text");
            View button = controller.get().findViewById(R.id.auth_guest);
            assertTrue("Guest button remains laid out inside scroll content", button.getHeight() >= 48);
            NestedScrollView scroll = controller.get().findViewById(R.id.auth_scroll);
            scroll.fullScroll(View.FOCUS_DOWN);
            Rect visible = new Rect();
            assertTrue("Guest action is reachable by scrolling", button.getGlobalVisibleRect(visible));
            assertTrue("Full touch target is visible", visible.height() >= 48);
            render(controller.get(), 800, 360, "login-landscape-actions");
        } finally {
            RuntimeEnvironment.setFontScale(1f);
        }
    }

    private static void render(Activity activity, int width, int height, String name) throws Exception {
        View decor = activity.getWindow().getDecorView();
        decor.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        decor.layout(0, 0, width, height);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        decor.draw(new Canvas(bitmap));
        File output = new File("build/outputs/f8-preview", name + ".png");
        output.getParentFile().mkdirs();
        try (FileOutputStream stream = new FileOutputStream(output)) {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream));
        } finally {
            bitmap.recycle();
        }
    }
}
