package ruhani.movie;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.FileInputStream;

public class MovieDetails extends AppCompatActivity {

    RelativeLayout rl;
    boolean isViewed;
    ImageView poster, check;
    TextView title, description, release, genre, director, stars;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_details);

        Intent intent = getIntent();
        String movietitle = intent.getStringExtra("Title");
        String moviedesc = intent.getStringExtra("Desc");
        String filename = getIntent().getStringExtra("Poster");
        Bitmap movieposter = null;
        try {
            FileInputStream is = this.openFileInput(filename);
            movieposter = BitmapFactory.decodeStream(is);
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        String movierelease = intent.getStringExtra("Release");
        String moviegenre = intent.getStringExtra("Genre");
        String moviedirector = intent.getStringExtra("Director");
        String moviestars = intent.getStringExtra("Stars");

        rl = (RelativeLayout) findViewById(R.id.fullview);
        poster = (ImageView) findViewById(R.id.poster);
        title = (TextView) findViewById(R.id.title);
        description = (TextView) findViewById(R.id.desc);
        release = (TextView) findViewById(R.id.release);
        genre = (TextView) findViewById(R.id.genre);
        director = (TextView) findViewById(R.id.director);
        stars = (TextView) findViewById(R.id.stars);

        check = (ImageView) findViewById(R.id.check);
        check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isViewed) {
                    check.setImageResource(R.drawable.unchecked);
                } else {
                    check.setImageResource(R.drawable.checked);
                }
                isViewed = !isViewed;
            }
        });

        title.setText(movietitle);
        description.setText(moviedesc);
        if(movieposter != null) {
            poster.setImageBitmap(movieposter);
        }
        release.setText("Release Date: " + movierelease);
        genre.setText("Genre: " + moviegenre);
        director.setText("Director: " + moviedirector);
        stars.setText("Stars: " + moviestars);

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        Bitmap bmp = Bitmap.createScaledBitmap(movieposter, size.x, size.y, true);
        bmp = getGrayscale(bmp);
        Drawable bd = new BitmapDrawable(getResources(), bmp);
        rl.setBackground(bd);
        rl.getBackground().setAlpha(75);
    }

    private Bitmap getBitmapFromByte(byte[] image) {
        return BitmapFactory.decodeByteArray(image , 0, image.length);
    }

    public Bitmap getGrayscale(Bitmap original) {
        int height = original.getHeight();
        int width = original.getWidth();
        Bitmap grayscale = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas c = new Canvas(grayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(original, 0, 0, paint);
        return grayscale;
    }

    @Override
    public void onBackPressed() {
        if (isViewed) {
            setResult(RESULT_OK);
        } else {
            setResult(RESULT_CANCELED);
        }
        finish();
    }

}
