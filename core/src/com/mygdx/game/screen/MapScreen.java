package com.mygdx.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.maps.MapLayers;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapRenderer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.mongodb.client.FindIterable;
import com.mygdx.game.BoatAnimation;
import com.mygdx.game.VirtualRunner;
import com.mygdx.game.lang.Context;
import com.mygdx.game.lang.Renderer;
import com.mygdx.game.utils.Constants;
import com.mygdx.game.utils.Geolocation;
import com.mygdx.game.utils.MapRasterTiles;
import com.mygdx.game.utils.MongoDBConnection;
import com.mygdx.game.utils.ZoomXY;

import org.bson.Document;
import org.bson.types.ObjectId;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapScreen extends ScreenAdapter implements GestureDetector.GestureListener{

    private MongoDBConnection mongoDBConnection;
    private ShapeRenderer shapeRenderer;
    private Vector3 touchPosition;
    private final VirtualRunner game;
    private final AssetManager assetManager;

    private TiledMap tiledMap;
    private TiledMapRenderer tiledMapRenderer;
    private OrthographicCamera camera;

    private Texture[] mapTiles;
    private ZoomXY beginTile;   // top left tile

    private SpriteBatch spriteBatch;

    // buttons
    private FitViewport hudViewport;
    private Stage hudStage;
    private Skin skin;
    private boolean showLangExample = false;
    private boolean weatherRain = false;
    private ParticleEffect rainParticleEffect;
    private boolean weatherSnow = false;
    private ParticleEffect snowParticleEffect;

    // animation
    Geolocation[] runCoordinates = {
            new Geolocation(46.5602f, 15.625186f),
            new Geolocation(46.5580f, 15.632482f),
            new Geolocation(46.5560f, 15.639112f),
            new Geolocation(46.5555f, 15.647974f),
            new Geolocation(46.5553f, 15.657766f)
    };
    BoatAnimation boatAnimation;

    // center geolocation

    public MapScreen(VirtualRunner game) {
        this.game = game;
        assetManager = game.getAssetManager();
    }


    public void createBoat() {
        boatAnimation = new BoatAnimation(runCoordinates, beginTile, 5);
        stage = new Stage(viewport, spriteBatch);
        stage.addActor(boatAnimation.create());
    }

    public void removeBoat() {
        Actor boatActor = stage.getRoot().findActor("boat");
        if (boatActor != null) {
            boatActor.remove();
        }
    }
    private Stage stage;
    private FitViewport viewport;
    public static Geolocation calculateCenter(Geolocation[] coordinates) {
        if (coordinates == null || coordinates.length == 0) {
            throw new IllegalArgumentException("Coordinates array is null or empty");
        }

        double maxLat = Double.MIN_VALUE;
        double minLat = Double.MAX_VALUE;
        double maxLng = Double.MIN_VALUE;
        double minLng = Double.MAX_VALUE;

        // Find the maximum and minimum latitude and longitude
        for (Geolocation geo : coordinates) {
            maxLat = Math.max(maxLat, geo.lat);
            minLat = Math.min(minLat, geo.lat);
            maxLng = Math.max(maxLng, geo.lng);
            minLng = Math.min(minLng, geo.lng);
        }

        // Calculate the center point
        double centerLat = (maxLat + minLat) / 2.0f;
        double centerLng = (maxLng + minLng) / 2.0f;

        return new Geolocation(centerLat, centerLng);
    }
    private Geolocation centerGeolocation = calculateCenter(runCoordinates);  // new Geolocation(46.557314, 15.637771);

    public void createMap() {
        try {
            ZoomXY centerTile = MapRasterTiles.getTileNumber(centerGeolocation.lat, centerGeolocation.lng, Constants.ZOOM);
            mapTiles = MapRasterTiles.getRasterTileZone(centerTile, Constants.NUM_TILES);
            beginTile = new ZoomXY(Constants.ZOOM, centerTile.x - ((Constants.NUM_TILES - 1) / 2), centerTile.y - ((Constants.NUM_TILES - 1) / 2));
        } catch (IOException e) {
            e.printStackTrace();
        }

        tiledMap = new TiledMap();
        MapLayers layers = tiledMap.getLayers();

        TiledMapTileLayer layer = new TiledMapTileLayer(Constants.NUM_TILES, Constants.NUM_TILES, MapRasterTiles.TILE_SIZE, MapRasterTiles.TILE_SIZE);
        int index = 0;
        for (int j = Constants.NUM_TILES - 1; j >= 0; j--) {
            for (int i = 0; i < Constants.NUM_TILES; i++) {
                TiledMapTileLayer.Cell cell = new TiledMapTileLayer.Cell();
                cell.setTile(new StaticTiledMapTile(new TextureRegion(mapTiles[index], MapRasterTiles.TILE_SIZE, MapRasterTiles.TILE_SIZE)));
                layer.setCell(i, j, cell);
                index++;
            }
        }
        layers.add(layer);

        tiledMapRenderer = new OrthogonalTiledMapRenderer(tiledMap);
    }

    @Override
    public void show() {
        shapeRenderer = new ShapeRenderer();

        // Create the MongoDB connection
        mongoDBConnection = new MongoDBConnection();

        camera = new OrthographicCamera();
        camera.setToOrtho(false, Constants.MAP_WIDTH, Constants.MAP_HEIGHT);
        camera.position.set(Constants.MAP_WIDTH / 2f, Constants.MAP_HEIGHT / 2f, 0);
        camera.viewportWidth = Constants.MAP_WIDTH / 2f;
        camera.viewportHeight = Constants.MAP_HEIGHT / 2f;
        camera.zoom = 2f;
        camera.update();

        spriteBatch = new SpriteBatch();
        hudViewport = new FitViewport(Constants.HUD_WIDTH, Constants.HUD_HEIGHT);
        viewport = new FitViewport(Constants.MAP_WIDTH / 2f, Constants.MAP_HEIGHT / 2f, camera);

        touchPosition = new Vector3();

//        TESTNO PRIDOBIVANJE PODATKOV
//        Document filterCriteria = new Document("location.coordinates", new Document("$exists", true));
//        Document element = mongoDBConnection.findDocuments(filterCriteria, "runs").first();
//
//        Document location = element.get("location", Document.class);
//        List<Double> coordinates = location.getList("coordinates",  Double.class);
//
//        double latitude =  coordinates.get(0);
//        double longitude =  coordinates.get(1);
//
//        System.out.println("Latitude: " + latitude);
//        System.out.println("Longitude: " + longitude);
//
//        Geolocation temp = new Geolocation( latitude, longitude);

        createMap();

        // buttons
        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
        hudStage = new Stage(hudViewport, spriteBatch);
        hudStage.addActor(createRunList());
        hudStage.addActor(createButtons());

        Gdx.input.setInputProcessor(new InputMultiplexer(hudStage, new GestureDetector(this)));

        // rain
        rainParticleEffect = new ParticleEffect();
        rainParticleEffect.load(Gdx.files.internal("particles/Rain.p"), Gdx.files.internal("particles"));
        rainParticleEffect.setPosition(0, Constants.MAP_HEIGHT); // Set the initial position above the screen
        rainParticleEffect.getEmitters().first().getSpawnWidth().setHigh(Constants.MAP_WIDTH);

        // snow
        snowParticleEffect = new ParticleEffect();
        snowParticleEffect.load(Gdx.files.internal("particles/SnowFlakes.p"), Gdx.files.internal("particles"));
        snowParticleEffect.setPosition(0, Constants.MAP_HEIGHT); // Set the initial position above the screen
        snowParticleEffect.getEmitters().first().getSpawnWidth().setHigh(Constants.MAP_WIDTH);

        // boat
        createBoat();
    }
    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);

        handleInput();

        camera.update();

        tiledMapRenderer.setView(camera);
        tiledMapRenderer.render();

        hudStage.act(Gdx.graphics.getDeltaTime());
        stage.act(Gdx.graphics.getDeltaTime());

        drawMarkers();

        hudStage.draw();
        stage.draw();


        // lang
        if(showLangExample){
            Renderer renderer = new Renderer();
            try {
                renderer.render(new FileInputStream(new File("program.txt")), new Context(shapeRenderer, camera, beginTile));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        // weather Rain
        if (weatherRain){
            spriteBatch.begin();

            // Update and draw the rain particle effect
            rainParticleEffect.update(Gdx.graphics.getDeltaTime());
            rainParticleEffect.draw(spriteBatch, Gdx.graphics.getDeltaTime());

            spriteBatch.end();
        }

        // weather Snow
        if (weatherSnow){
            spriteBatch.begin();

            snowParticleEffect.update(Gdx.graphics.getDeltaTime());
            snowParticleEffect.draw(spriteBatch, Gdx.graphics.getDeltaTime());

            spriteBatch.end();
        }
    }
    private void drawMarkers() {


        Vector2 marker = MapRasterTiles.getPixelPosition(centerGeolocation.lat, centerGeolocation.lng, beginTile.x, beginTile.y);

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.setColor(Color.BLUE);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.circle(marker.x, marker.y, 20);
        shapeRenderer.end();

        // boat positions
        for(int i=0; i<boatAnimation.getInterpolatedPositions().length; i++){
            shapeRenderer.setProjectionMatrix(camera.combined);
            shapeRenderer.setColor(Color.BLACK);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.circle(boatAnimation.getInterpolatedPositions()[i].x, boatAnimation.getInterpolatedPositions()[i].y, 10);
            shapeRenderer.end();
        }
    }
    public void updateCoordinates(String runId) {
        int reduction = 10;
        FindIterable<Document> documents = mongoDBConnection.findDocuments(new Document("_id", new ObjectId(runId)), "runs");
        Document runDocument = documents.first();
        if (runDocument != null) {
            Document stream = (Document) runDocument.get("stream");
            if (stream != null) {
                Document latlng = (Document) stream.get("latlng");
                if (latlng != null) {
                    List<List<Double>> data = (List<List<Double>>) latlng.get("data");
                    if (data != null) {
                        runCoordinates = new Geolocation[data.size() / reduction];
                        List<Geolocation> reducedCoordinates = new ArrayList<>();
                        for (int i = 0; i < data.size(); i += reduction) {
                            List<Double> latlngData = data.get(i);
                            reducedCoordinates.add(new Geolocation(latlngData.get(0), latlngData.get(1)));
                        }
                        runCoordinates = reducedCoordinates.toArray(new Geolocation[0]);
                    }
                }
            }
        }
        centerGeolocation = calculateCenter(runCoordinates);
        boatAnimation.setGeolocations(runCoordinates, beginTile, 5);
        removeBoat();
        createMap();
        createBoat();
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
        hudStage.dispose();
        mongoDBConnection.closeConnection();
    }
    @Override
    public boolean touchDown(float x, float y, int pointer, int button) {
        touchPosition.set(x, y, 0);
        camera.unproject(touchPosition);
        return false;
    }
    @Override
    public boolean tap(float x, float y, int count, int button) {
        return false;
    }

    @Override
    public boolean longPress(float x, float y) {
        return false;
    }

    @Override
    public boolean fling(float velocityX, float velocityY, int button) {
        return false;
    }

    @Override
    public boolean pan(float x, float y, float deltaX, float deltaY) {
        camera.translate(-deltaX, deltaY);
        return false;
    }

    @Override
    public boolean panStop(float x, float y, int pointer, int button) {
        return false;
    }

    @Override
    public boolean zoom(float initialDistance, float distance) {
        if (initialDistance >= distance)
            camera.zoom += 0.02;
        else
            camera.zoom -= 0.02;
        return false;
    }

    @Override
    public boolean pinch(Vector2 initialPointer1, Vector2 initialPointer2, Vector2 pointer1, Vector2 pointer2) {
        return false;
    }

    @Override
    public void pinchStop() {

    }

    private void handleInput() {
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            camera.zoom += 0.02;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.Q)) {
            camera.zoom -= 0.02;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            camera.translate(-3, 0, 0);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            camera.translate(3, 0, 0);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            camera.translate(0, -3, 0);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
            camera.translate(0, 3, 0);
        }

        camera.zoom = MathUtils.clamp(camera.zoom, 0.5f, 2f);

        float effectiveViewportWidth = camera.viewportWidth * camera.zoom;
        float effectiveViewportHeight = camera.viewportHeight * camera.zoom;

        camera.position.x = MathUtils.clamp(camera.position.x, effectiveViewportWidth / 2f, Constants.MAP_WIDTH - effectiveViewportWidth / 2f);
        camera.position.y = MathUtils.clamp(camera.position.y, effectiveViewportHeight / 2f, Constants.MAP_HEIGHT - effectiveViewportHeight / 2f);
    }


    private Actor createRunList() {
        Table table = new Table();
        table.defaults().pad(5);

        List<String> runNames = new ArrayList<>();
        List<String> runIds = new ArrayList<>();

        FindIterable<Document> documents = mongoDBConnection.findDocuments(new Document(), "runs");
        for (Document document : documents) {
            Document activity = (Document) document.get("activity");
            if (activity != null) {
                String name = activity.getString("name");
                ObjectId id = document.getObjectId("_id");

                if (name != null) {
                    runNames.add(name);
                    runIds.add(id.toHexString());
                }
            }
        }

        ScrollPane scrollPane = new ScrollPane(table, skin);
        scrollPane.setFlickScroll(true);

        for (int i = 0; i < runNames.size(); i++) {
            final String runName = runNames.get(i);
            final String runId = runIds.get(i);
            TextButton runButton = new TextButton(runName, skin);
            runButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    Gdx.app.log("Run Clicked", runName);
                    updateCoordinates(runId);
                }
            });
            table.add(runButton).padBottom(15).expandX().fill().row();
        }

        Table listTable = new Table();
        listTable.add(scrollPane).fill().width(Constants.HUD_WIDTH / 3.0f).height(Constants.HUD_HEIGHT / 2.0f);
        listTable.bottom();
        listTable.left();
        listTable.setFillParent(true);
        listTable.pack();

        return listTable;
    }


    private Actor createButtons() {
        Table table = new Table();
        table.defaults().pad(20);

        TextButton langButton = new TextButton("Lang", skin, "toggle");
        langButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showLangExample = !showLangExample;
            }
        });

        TextButton animButton = new TextButton("Animation", skin);
        animButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                stage.addActor(boatAnimation.create());
            }
        });

        TextButton rain = new TextButton("Rain", skin);
        rain.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if(weatherSnow)
                    weatherSnow = false;
                weatherRain = !weatherRain;
            }
        });

        TextButton snow = new TextButton("Snow", skin);
        snow.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if(weatherRain)
                    weatherRain = false;
                weatherSnow = !weatherSnow;
            }
        });

        TextButton quitButton = new TextButton("Quit", skin);
        quitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });

        Table buttonTable = new Table();
        buttonTable.defaults().padLeft(30).padRight(30);

        buttonTable.add(langButton).padBottom(15).expandX().fill().row();
        buttonTable.add(animButton).padBottom(15).fillX().row();
        buttonTable.add(rain).padBottom(15).fillX().row();
        buttonTable.add(snow).padBottom(15).fillX().row();
        buttonTable.add(quitButton).fillX();

        table.add(buttonTable);
        table.left();
        table.top();
        table.setFillParent(true);
        table.pack();

        return table;
    }
}