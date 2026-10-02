package jp.shint.terracraft.portablerecipe;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Presentation only: inventory validation and crafting stay on the server. */
public final class PortableRecipeScreen extends Screen {
    private static final float BOOKSHELF_SCALE = 1.37F;
    /** Fits the open book to the supplied full-screen reference. */
    private static final float DETAIL_SCALE = 1.19F;
    private static final Identifier BOOKSHELF_REFERENCE = Identifier.of(
            PortableRecipeMod.MOD_ID, "textures/gui/bookshelf_reference.png");
    private static final Identifier RECIPE_BOOK_REFERENCE = Identifier.of(
            PortableRecipeMod.MOD_ID, "textures/gui/recipe_book_reference.png");
    private static final Identifier NETHER_RECIPE_BOOK = Identifier.of(
            PortableRecipeMod.MOD_ID, "textures/gui/recipe_book_nether.png");
    private static final Identifier CRAFT_PERMISSION = Identifier.of(
            PortableRecipeMod.MOD_ID, "textures/gui/craft_permission.png");
    private static final int REFERENCE_WIDTH = 1672, REFERENCE_HEIGHT = 941;
    private static final int SHELF_SOURCE_X = 206, SHELF_SOURCE_Y = 56, SHELF_SOURCE_WIDTH = 1260, SHELF_SOURCE_HEIGHT = 819;
    private static final int BOOK_TEXTURE_WIDTH = 1671, BOOK_TEXTURE_HEIGHT = 941;
    private static final int BOOK_SOURCE_X = 0, BOOK_SOURCE_Y = 0, BOOK_SOURCE_WIDTH = 1671, BOOK_SOURCE_HEIGHT = 941;
    private static final int INK=0xFF382516, PAPER=0xFFF2DEB2, DARK=0xFF25150D, WOOD=0xFF6D3A1C;
    private static final int LIGHT_WOOD=0xFF9B5A2B, GOLD=0xFFF1C65B, GOLD_DARK=0xFF9C641A, RED=0xFFC54036;
    private Page page=Page.BOOKSHELF;
    private int craftAnimationTicks;
    private int observedSuccessVersion;

    public PortableRecipeScreen(){ super(Text.translatable("screen.terracraft_portable_recipe_ui.title")); }

    @Override public void render(DrawContext c,int mx,int my,float delta){
        int latestSuccessVersion=PortableRecipeClient.successVersion();
        if(latestSuccessVersion!=observedSuccessVersion){observedSuccessVersion=latestSuccessVersion;craftAnimationTicks=30;}
        else if(craftAnimationTicks>0)craftAnimationTicks--;
        c.fill(0,0,width,height,0xB8000000);
        float scale = page == Page.BOOKSHELF ? BOOKSHELF_SCALE : DETAIL_SCALE;
        // Scale around the screen centre so the visible UI and its hit targets agree.
        float offsetY = page == Page.DETAIL ? height * 0.022F : 0F;
        c.getMatrices().pushMatrix();
        c.getMatrices().translate(width / 2F, height / 2F + offsetY);
        c.getMatrices().scale(scale, scale);
        c.getMatrices().translate(-width / 2F, -height / 2F);
        if(page==Page.BOOKSHELF) shelf(c,shelf(),mx,my); else detail(c,book(),mx,my);
        c.getMatrices().popMatrix();
    }

    private void shelf(DrawContext c, Box b,int mx,int my){
        int x=b.x(),y=b.y(),w=b.w(),h=b.h();
        Box book=recipeBook(b);
        int lift=screenHit(mx,my,book,BOOKSHELF_SCALE)?Math.max(3,h/30):0;
        // Exact pixel crop from the supplied PNG; the live world remains outside this rectangle.
        c.drawTexture(RenderPipelines.GUI_TEXTURED, BOOKSHELF_REFERENCE, x, y,
                SHELF_SOURCE_X, SHELF_SOURCE_Y, w, h,
                SHELF_SOURCE_WIDTH, SHELF_SOURCE_HEIGHT, REFERENCE_WIDTH, REFERENCE_HEIGHT);
        // Preserve hover feedback and the original clickable recipe-book area.
        if(lift>0) c.fill(book.x()-2,book.bottom()-lift,book.right()+2,book.bottom()-lift+2,0x88F4C66B);
    }

    private void lantern(DrawContext c,int cx,int y,int s){ int d=s/2;
        c.fill(cx-d/3,y-s/2,cx+d/3,y,GOLD_DARK); c.fill(cx-d,y,cx+d,y+s,0xFF3D281A);
        c.fill(cx-d+3,y+3,cx+d-3,y+s-3,0xFFFFB93E); c.fill(cx-d+6,y+6,cx+d-6,y+s-6,0xFFFFEF9A);
        c.fill(cx-d-2,y+s,cx+d+2,y+s+4,0xFF25170E);
    }

    private void plaque(DrawContext c,int x,int y,int w,int h){ c.fill(x-3,y-3,x+w+3,y+h+3,0xFF1A0D08); c.fill(x,y,x+w,y+h,0xFF56321C); c.fill(x+3,y+3,x+w-3,y+h-3,0xFF3A2113); c.fill(x+5,y+5,x+8,y+8,0xFFAD7839); c.fill(x+w-8,y+5,x+w-5,y+8,0xFFAD7839); c.fill(x+5,y+h-8,x+8,y+h-5,0xFFAD7839); c.fill(x+w-8,y+h-8,x+w-5,y+h-5,0xFFAD7839); }
    private void corner(DrawContext c,int x,int y,int size){ c.fill(x,y,x+size,y+size,0xFFDCAC4A); c.fill(x+2,y+2,x+size,y+size,0xFF9B621E); }
    private void books(DrawContext c,int x,int y,int h){ c.fill(x,y,x+8,y+h,0xFF70301B); c.fill(x+2,y+3,x+6,y+h-3,0xFFA65C27); c.fill(x+9,y+2,x+17,y+h,0xFF1D463C); c.fill(x+11,y+4,x+15,y+h-2,0xFF396A55); }

    private void detail(DrawContext c,Box b,int mx,int my){
        int x=b.x(),y=b.y(),w=b.w(),h=b.h(),leftCenter=x+w*31/100,rightCenter=x+w*70/100;
        // The supplied reference is a transparent book cutout with gameplay fields cleared.
        c.drawTexture(RenderPipelines.GUI_TEXTURED, NETHER_RECIPE_BOOK, x, y,
                BOOK_SOURCE_X, BOOK_SOURCE_Y, w, h,
                BOOK_SOURCE_WIDTH, BOOK_SOURCE_HEIGHT, BOOK_TEXTURE_WIDTH, BOOK_TEXTURE_HEIGHT);
        PortableRecipeClient.RecipeViewState s=PortableRecipeClient.state();
        centerScaled(c,Text.translatable("screen.terracraft_portable_recipe_ui.recipe_name"),leftCenter,y+h*15/100,INK,1.05F);
        centerScaled(c,Text.translatable("screen.terracraft_portable_recipe_ui.materials"),rightCenter,y+h*15/100,INK,1.05F);
        if(!s.loaded()){ centerScaled(c,Text.literal(s.message()),leftCenter,y+h*43/100,PAPER,1F); return; }
        if(!s.unlocked()){
            centerScaled(c,Text.translatable("screen.terracraft_portable_recipe_ui.locked"),leftCenter,y+h*43/100,0xFFFF7770,1F);
        }else{
            previewCentered(c,new ItemStack(TerracraftAccess.terraAxe()),leftCenter,y+h*43/100,Math.max(1F,w/110F));
            centerScaled(c,Text.translatable("screen.terracraft_portable_recipe_ui.preview"),leftCenter,y+h*66/100,0xFF382516,1F);
        }
        int matX=x+w*53/100,right=x+w*84/100;
        material(c,matX,y+h*29/100,right,new ItemStack(Items.NETHERITE_INGOT),"ネザライトインゴット",3,s.netheriteCount());
        material(c,matX,y+h*43/100,right,new ItemStack(Items.BLAZE_ROD),"ブレイズロッド",2,s.blazeRodCount());
        ritualSigil(c,matX,y+h*58/100,Math.max(16,h*10/100));
        centerScaled(c,Text.literal(s.unlocked()?"クラフト権利：取得済み":"クラフト権利：未取得"),x+w*71/100,y+h*58/100,s.unlocked()?INK:RED,1F);
        boolean can=s.unlocked()&&s.netheriteCount()>=3&&s.blazeRodCount()>=2;
        if(!can) centerScaled(c,Text.literal(reason(s)),rightCenter,y+h*68/100,RED,0.8F); else if(!s.message().isBlank()) centerScaled(c,Text.literal(s.message()),rightCenter,y+h*68/100,INK,0.8F);
        button(c,back(b),mx,my,Text.translatable("screen.terracraft_portable_recipe_ui.back"),true,false,DETAIL_SCALE);
        button(c,craft(b),mx,my,Text.translatable("screen.terracraft_portable_recipe_ui.craft"),can,true,DETAIL_SCALE);
    }

    private void material(DrawContext c,int x,int centerY,int right,ItemStack item,String name,int need,int own){
        boolean ok=own>=need;
        preview(c,item,x,centerY-8,1F);
        leftScaled(c,Text.literal(name),x+20,centerY,INK,1F);
        rightScaled(c,Text.literal(own+" / "+need),right-5,centerY+9,ok?INK:RED,0.5F);
    }
    private void button(DrawContext c,Box b,int mx,int my,Text label,boolean active,boolean dimWhenInactive,float scale){
        // The reference already contains the wood, metal corners, and gold craft plate.
        // Only tint an unavailable craft action; never paint a generic rectangle over it.
        boolean hover=screenHit(mx,my,b,scale);
        if (!active && dimWhenInactive) c.fill(b.x()+4,b.y()+4,b.right()-4,b.bottom()-4,0x880E0906);
        if (hover && active) c.fill(b.x()+4,b.y()+4,b.right()-4,b.bottom()-4,0x22FFF0A0);
        if(dimWhenInactive&&craftAnimationTicks>0){
            float progress=1F-craftAnimationTicks/30F;
            int pulse=(int)(3+Math.sin(progress*Math.PI*8)*3);
            c.fill(b.x()+pulse,b.y()+pulse,b.right()-pulse,b.bottom()-pulse,0x88FFD54A);
            if(craftAnimationTicks%6<3)c.fill(b.x()+2,b.y()+2,b.right()-2,b.y()+4,0xFFFFE486);
        }
        centerScaled(c,label,b.x()+b.w()/2,b.y()+b.h()/2,active?INK:0xFFF4E4C5,1F);
    }
    private void preview(DrawContext c,ItemStack item,int x,int y,float scale){ c.getMatrices().pushMatrix(); c.getMatrices().translate(x,y); c.getMatrices().scale(scale,scale); c.drawItem(item,0,0); c.getMatrices().popMatrix(); }
    private void previewCentered(DrawContext c,ItemStack item,int centerX,int centerY,float scale){ c.getMatrices().pushMatrix(); c.getMatrices().translate(centerX,centerY); c.getMatrices().scale(scale,scale); c.drawItem(item,-8,-8); c.getMatrices().popMatrix(); }
    private void centerScaled(DrawContext c,Text value,int centerX,int centerY,int color,float scale){
        c.getMatrices().pushMatrix(); c.getMatrices().translate(centerX,centerY); c.getMatrices().scale(scale,scale);
        c.drawText(textRenderer,value,-textRenderer.getWidth(value)/2,-4,color,false); c.getMatrices().popMatrix();
    }
    private void leftScaled(DrawContext c,Text value,int left,int centerY,int color,float scale){
        c.getMatrices().pushMatrix(); c.getMatrices().translate(left,centerY); c.getMatrices().scale(scale,scale);
        c.drawText(textRenderer,value,0,-4,color,false); c.getMatrices().popMatrix();
    }
    private void rightScaled(DrawContext c,Text value,int right,int centerY,int color,float scale){
        c.getMatrices().pushMatrix(); c.getMatrices().translate(right,centerY); c.getMatrices().scale(scale,scale);
        c.drawText(textRenderer,value,-textRenderer.getWidth(value),-4,color,false); c.getMatrices().popMatrix();
    }
    private void ritualSigil(DrawContext c,int x,int centerY,int size){
        c.drawTexture(RenderPipelines.GUI_TEXTURED,CRAFT_PERMISSION,x,centerY-size/2,
                0,0,size,size,1254,1254,1254,1254);
    }
    private String reason(PortableRecipeClient.RecipeViewState s){ if(!s.unlocked())return "解放条件を満たしていません"; if(s.netheriteCount()<3)return "不足：ネザライト "+(3-s.netheriteCount())+"個"; return "不足：ブレイズロッド "+(2-s.blazeRodCount())+"本"; }
    private void text(DrawContext c,Text v,int x,int y,int color){ c.drawText(textRenderer,v,x,y,color,false); }
    private void center(DrawContext c,Text v,int x,int y,int color){ text(c,v,x-textRenderer.getWidth(v)/2,y,color); }

    @Override public boolean mouseClicked(Click click,boolean doubleClicked){ if(click.button()!=0)return super.mouseClicked(click,doubleClicked); Box s=shelf(),b=book();
        if(page==Page.BOOKSHELF&&screenHit(click.x(),click.y(),recipeBook(s),BOOKSHELF_SCALE)){page=Page.DETAIL;return true;}
        if(page==Page.DETAIL&&screenHit(click.x(),click.y(),back(b),DETAIL_SCALE)){page=Page.BOOKSHELF;return true;}
        if(page==Page.DETAIL&&screenHit(click.x(),click.y(),craft(b),DETAIL_SCALE)){
            PortableRecipeClient.RecipeViewState state=PortableRecipeClient.state();
            if(state.unlocked()&&state.netheriteCount()>=3&&state.blazeRodCount()>=2){ClientPlayNetworking.send(new PortableRecipeNetworking.CraftTerraAxePayload());}
            return true;
        }
        return super.mouseClicked(click,doubleClicked);
    }
    @Override public boolean shouldCloseOnEsc(){return true;}
    private static boolean hit(double mx,double my,int x,int y,int w,int h){return mx>=x&&mx<x+w&&my>=y&&my<y+h;}
    /** Hit-test against the exact screen-space rectangle produced by the matrix transform. */
    private boolean screenHit(double mx,double my,Box design,float scale){
        double left=width/2.0+(design.x()-width/2.0)*scale;
        double offsetY=page==Page.DETAIL?height*0.022:0;
        double top=height/2.0+offsetY+(design.y()-height/2.0)*scale;
        double right=left+design.w()*scale;
        double bottom=top+design.h()*scale;
        // Allow a few GUI pixels for framebuffer-to-GUI rounding at non-integer scales.
        return mx>=left-3&&mx<right+3&&my>=top-3&&my<bottom+3;
    }
    private Box shelf(){int w=Math.min(Math.max(180,width*55/100),width-24),h=w*SHELF_SOURCE_HEIGHT/SHELF_SOURCE_WIDTH;return new Box(width/2-w/2,height/2-h/2,w,h);}
    private Box book(){int w=Math.min(Math.max(280,width*72/100),width-24),h=Math.min(w*BOOK_SOURCE_HEIGHT/BOOK_SOURCE_WIDTH,height-34);return new Box(width/2-w/2,height/2-h/2,w,h);}
    /**
     * The visible cover is 41–60% by 33–67% of the crop.  The extra cover-edge
     * tolerance keeps input aligned after Minecraft rounds GUI-scale coordinates.
     */
    private Box recipeBook(Box b){return new Box(b.x()+b.w()*38/100,b.y()+b.h()*29/100,b.w()*25/100,b.h()*43/100);}
    /** Pixel-measured plates in the 1008 x 611 open-book crop. */
    private Box back(Box b){return new Box(b.x()+b.w()*17/100,b.y()+b.h()*73/100,b.w()*20/100,b.h()*10/100);}
    private Box craft(Box b){return new Box(b.x()+b.w()*58/100,b.y()+b.h()*69/100,b.w()*24/100,b.h()*13/100);}
    private record Box(int x,int y,int w,int h){int right(){return x+w;}int bottom(){return y+h;}boolean contains(double x,double y){return hit(x,y,x(),y(),w,h);}}
    private enum Page{BOOKSHELF,DETAIL}
}
