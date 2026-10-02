package jp.shint.terracraft.jei;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;

/** Standalone geometry checks and a software preview using the same camera/UV transform. */
public final class RitualProjectionCheck {
    private static final float[] U = {0, 0, 1, 1}, V = {0, 1, 1, 0};
    private static final String[] FLOOR = {"GCRBG", "CBBRM", "RRCMM", "MRBRC", "GMBCG"};
    private static final int[][] NORMALS = {{0,1,0},{1,0,0},{0,0,1},{-1,0,0},{0,0,-1}};
    private static final float[][][] VERTICES = {
        {{0,1,0},{0,1,1},{1,1,1},{1,1,0}},
        {{1,1,0},{1,0,0},{1,0,1},{1,1,1}},
        {{1,1,1},{1,0,1},{0,0,1},{0,1,1}},
        {{0,1,1},{0,0,1},{0,0,0},{0,1,0}},
        {{0,1,0},{0,0,0},{1,0,0},{1,1,0}}
    };
    record Face(char symbol, int direction, RitualProjection.Point[] points, float depth) { }

    public static void main(String[] args) throws Exception {
        for (int degrees = 0; degrees < 360; degrees += 5) {
            var camera = new RitualProjection(Math.toRadians(degrees));
            check(camera.facing(0,1,0) > 0 && camera.facing(0,-1,0) < 0, "camera must stay above floor");
            for (Face face : faces(camera, false)) {
                for (var p : face.points) check(p.x() >= 1 && p.x() <= 205 && p.y() >= 1 && p.y() <= 91, "scene clipped at " + degrees);
                float x = 0, y = 0;
                for (var p : face.points) { x += p.x()/4; y += p.y()/4; }
                check(RitualProjection.contains(face.points, x, y), "face centroid must be pickable");
                check(!RitualProjection.contains(face.points, -100, -100), "outside point must not pick a face");
                for (int rotation = 0; rotation < 4; rotation++) for (int flip = 0; flip < 2; flip++) {
                    float[] u = new float[4], v = new float[4];
                    for (int i = 0; i < 4; i++) { u[i] = U[(i+rotation)%4]; v[i] = V[(i+rotation)%4]; if (flip == 1) u[i] = 1-u[i]; }
                    float[] m = RitualProjection.faceTransform(face.points, u, v);
                    check(m != null, "valid UV basis");
                    for (int i = 0; i < 4; i++) {
                        check(Math.abs(m[0]*u[i]+m[2]*v[i]+m[4]-face.points[i].x()) < 0.001, "UV x mismatch");
                        check(Math.abs(m[1]*u[i]+m[3]*v[i]+m[5]-face.points[i].y()) < 0.001, "UV y mismatch");
                    }
                }
            }
            List<Face> floor = faces(camera, true).stream().filter(f -> f.direction == 0).toList();
            check(floor.size() == 25, "floor-only mode must expose all 25 top faces");
            for (Face face : floor) {
                double cx = 0, cy = 0;
                for (var p : face.points) { cx += p.x()/4; cy += p.y()/4; }
                int hits = 0;
                for (Face other : floor) if (RitualProjection.contains(other.points, cx, cy)) hits++;
                check(hits == 1, "floor picking must not select an adjacent block");
            }
            var coreTop = camera.project(0,2,0);
            Face picked = null;
            for (Face face : faces(camera, false)) if (RitualProjection.contains(face.points, coreTop.x(), coreTop.y())) picked = face;
            check(picked != null && picked.symbol == 'A', "core must occlude the floor");
        }
        if (args.length == 2) preview(Path.of(args[0]), Path.of(args[1]));
        System.out.println("PASS: 72 camera angles, UV rotations/mirroring, viewport bounds, floor picking and core occlusion");
    }

    private static boolean occupied(int x, int y, int z, boolean floorOnly) {
        return y == 0 && x >= 0 && x < 5 && z >= 0 && z < 5 || !floorOnly && y == 1 && x == 2 && z == 2;
    }
    private static List<Face> faces(RitualProjection camera, boolean floorOnly) {
        List<Face> faces = new ArrayList<>();
        for (int y=0;y<2;y++) for (int z=0;z<5;z++) for (int x=0;x<5;x++) {
            if (!occupied(x,y,z,floorOnly)) continue;
            for (int d=0;d<NORMALS.length;d++) {
                int[] n = NORMALS[d];
                if (camera.facing(n[0],n[1],n[2]) <= 0.001 || occupied(x+n[0],y+n[1],z+n[2],floorOnly)) continue;
                var points = new RitualProjection.Point[4]; float depth = 0;
                for (int i=0;i<4;i++) { float[] v = VERTICES[d][i]; points[i] = camera.project(x+v[0]-2.5F,y+v[1],z+v[2]-2.5F); depth += points[i].depth()/4; }
                faces.add(new Face(y==1?'A':FLOOR[z].charAt(x),d,points,depth));
            }
        }
        faces.sort(Comparator.comparingDouble(Face::depth));
        return faces;
    }

    private static void preview(Path minecraftJar, Path output) throws Exception {
        Map<Character,String> textures = Map.of('G',"gilded_blackstone",'C',"crying_obsidian",'A',"crying_obsidian",'B',"polished_blackstone_bricks",'R',"cracked_polished_blackstone_bricks",'M',"magma");
        BufferedImage image = new BufferedImage(824,440,BufferedImage.TYPE_INT_ARGB);
        var g = image.createGraphics(); g.scale(4,4);
        g.setColor(new Color(0x171D26));g.fillRect(0,0,206,110);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        try (ZipFile zip = new ZipFile(minecraftJar.toFile())) {
            for (Face face : faces(new RitualProjection(Math.PI/4),false)) {
                var entry = zip.getEntry("assets/minecraft/textures/block/"+textures.get(face.symbol)+".png");
                BufferedImage sprite;
                try (var in = zip.getInputStream(entry)) { sprite = ImageIO.read(in); }
                int size = sprite.getWidth(); sprite = sprite.getSubimage(0,0,size,size);
                float[] m = RitualProjection.faceTransform(face.points,U,V);
                AffineTransform old = g.getTransform();
                g.transform(new AffineTransform(m[0],m[1],m[2],m[3],m[4],m[5]));
                g.drawImage(sprite,0,0,1,1,null);
                int shade = face.direction==0?0:face.direction==1||face.direction==3?50:80;
                g.setColor(new Color(0,0,0,shade));g.fillRect(0,0,1,1);
                g.setTransform(old);
            }
        }
        g.dispose(); ImageIO.write(image,"PNG",output.toFile());
    }
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
