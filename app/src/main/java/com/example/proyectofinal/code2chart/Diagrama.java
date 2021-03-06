package com.example.proyectofinal.code2chart;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.mindfusion.diagramming.ContainerNode;
import com.mindfusion.diagramming.Diagram;
import com.mindfusion.diagramming.DiagramNode;
import com.mindfusion.diagramming.DiagramView;
import com.mindfusion.diagramming.FitSize;
import com.mindfusion.diagramming.LayeredLayout;
import com.mindfusion.diagramming.ShadowsStyle;
import com.mindfusion.diagramming.ShapeNode;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import parserUtils.ASTContainer;
import parserUtils.AbstractSyntaxTreeConverter;
import parserUtils.CCompiler;
import parserUtils.MyCVisitor;
import parserUtils.ParserToXmlAdapter;
import parserUtils.XmlBuilder;
import uk.co.senab.photoview.PhotoViewAttacher;

public class Diagrama extends AppCompatActivity implements View.OnClickListener {

    private DiagramView diagramView;
    private ImageButton guardar, descartar;
    private Diagram diagram;

    private ArrayList<ContainerNode> listaDeBucles = new ArrayList<>();
    private ArrayList<ArrayList<Object>> listasDeNodosParaBucles = new ArrayList<>();

    private ImageView imagen;
    private HashMap<String, DiagramNode> nodeMap = new HashMap<>();
    private RectF medidaDiagrama;

    private PhotoViewAttacher pAttacher;

    private ArrayList<String> nodosDecision = new ArrayList<>();
    private ArrayList<String> nodosNoDecision = new ArrayList<>();

    private NodoHandler manejador = new NodoHandler();

    private int cantidadTotalDeBucles = 0;

    private String uri, titulo, autor, codigo;

    private Bitmap imagenPrincipal;

    private String xmlName = null;
    String magia = null;
    AlertDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diagrama);

        diagramView = (DiagramView)findViewById(R.id.diag_view);

        descartar = (ImageButton) findViewById(R.id.descartar);
        descartar.setOnClickListener(this);

        guardar = (ImageButton) findViewById(R.id.guardar);
        guardar.setOnClickListener(this);

        imagen = (ImageView) findViewById(R.id.imagen);
        pAttacher = new PhotoViewAttacher(imagen);
        pAttacher.update();

        Bundle bundleDiagrama = getIntent().getExtras();
        if(bundleDiagrama != null){
            uri = bundleDiagrama.getString("uriDelArchivo");
            titulo = bundleDiagrama.getString("tituloMando");
            autor = bundleDiagrama.getString("autorMando");
            codigo = bundleDiagrama.getString("codigo");
        }

        diagram = diagramView.getDiagram();

        diagram.setShadowsStyle(ShadowsStyle.None);

        progressBar();

        (new AsyncTask<Void, Void, Void>() {

            @Override
            protected void onPreExecute() {
                try {
                    magia = magia(uri);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                super.onPreExecute();
            }

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    loadGraph(magia, diagram);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                armarElLayout(diagram);

                for(int i = listaDeBucles.size()-1; i >= 0; --i){
                    for (int k = 0; k < listasDeNodosParaBucles.get(i).size(); ++k) {
                        listaDeBucles.get(i).add((DiagramNode) listasDeNodosParaBucles.get(i).get(k));
                    }
                    armarElLayout(diagram);
                }
                dialog.dismiss();
                super.onPostExecute(aVoid);
            }
        }).execute();


    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus) {
            diagramView.zoomToFit();
            imagenPrincipal = diagram.createImage();
            imagen.setImageBitmap(imagenPrincipal);
        }
    }

    public String magia(String unaUri) throws Exception {
        xmlName = new String(Environment.getExternalStorageDirectory()+File.separator+"xml");
        String filePreParse = "";
        if(codigo.equals("vieneVacío")) {
            Uri myUri = Uri.parse(unaUri);
            InputStream inputStream = getContentResolver().openInputStream(myUri);
            filePreParse = convertStreamToString(inputStream);
        }else{
            filePreParse = codigo;
        }
        CCompiler compiler = new CCompiler();
        AbstractSyntaxTreeConverter ast = compiler.compile(filePreParse);

        MyCVisitor visitor = new MyCVisitor();
        visitor.visit(ast,null);

        ParserToXmlAdapter adapter = new ParserToXmlAdapter();
        LinkedList<ASTContainer> list = adapter.getConvertedList(ast);

        XmlBuilder builder = new XmlBuilder(xmlName);
        builder.setXmlStructure();

        for(int i = 0 ; i<list.size(); ++i){
            builder.appendNode(list.get(i).getId(), list.get(i).getTipo(), list.get(i).getContent());
            if (list.get(i).getTipo() == "decisión") {
                builder.appendLink(list.get(i).getFather(), list.get(i).getId(), "decision");
            } else {
                builder.appendLink(list.get(i).getFather(), list.get(i).getId(), "");
            }
        }

        builder.build();

        String resultado = builder.getFile().getAbsolutePath();

        return resultado;
    }

    public String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    public void loadGraph(String filepath, Diagram diagram) {

        RectF bounds = new RectF(0, 0, 20, 10);

        // load the graph xml
        Document document = loadXmlFile(filepath);
        Element root = document.getDocumentElement();

        //traigo todos los nodos, y todos los links
        NodeList nodes = root.getElementsByTagName("Node");
        NodeList links = root.getElementsByTagName("Link");

        RectF medidaIncial = new RectF(0, 0, 5000, 5000);
        diagram.setBounds(medidaIncial);

        dibujarLosNodosYClasificarlos(nodes, bounds);

        List<String> nodosYaLinkeados = new ArrayList<String>(); //para mapear de 1 sola vez
        //mapeo los links
        for (int i = 0; i < links.getLength(); ++i) {

            Element link = (Element) links.item(i);
            DiagramNode origin = nodeMap.get(link.getAttribute("origin"));
            if (!esNodoDecision(link.getAttribute("origin"), nodosDecision)) {
                //es un nodo comun
                DiagramNode target = nodeMap.get(link.getAttribute("target"));
                diagram.getFactory().createDiagramLink(origin, target);
                nodosYaLinkeados.add(link.getAttribute("origin"));
            } else {
                //primero me fijo si ya fueron mapeados sus links
                //entrando a esta parte significa que es un nodo de decision
                if (!nodosYaLinkeados.contains(link.getAttribute("origin"))) {
                    List<String> idsTarget = new ArrayList<String>();
                    idsTarget = obtenerNodosTargetsDadoUnNodoOrigenDeDecision(link.getAttribute("origin"), links);
                    DiagramNode target1 = nodeMap.get(idsTarget.get(0));
                    DiagramNode target2 = nodeMap.get(idsTarget.get(1));
                    diagram.getFactory().createDiagramLink(origin, target1).setText("SI");
                    if(target2!=null) {
                        diagram.getFactory().createDiagramLink(origin, target2).setText("NO");
                    }
                    nodosYaLinkeados.add(link.getAttribute("origin"));
                }

            }
        }

    }

    public Document loadXmlFile(String filepath){
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringElementContentWhitespace(true);
        factory.setNamespaceAware(true);

        Document document = null;
        DocumentBuilder builder;
        try{
            File file = new File(filepath);
            builder = factory.newDocumentBuilder();
            document = builder.parse(file);
        }catch (Exception e){
            e.printStackTrace();
        }
        return document;
    }

    public void dibujarLosNodosYClasificarlos(NodeList nodes, RectF bounds){

        String idBucle = null;
        int enDonde = 0;

        for (int i = 0; i < nodes.getLength(); ++i) {

            Element node = (Element) nodes.item(i);
            //nuevocodigo
            String tipo = node.getAttribute("tipo");
            switch (tipo) {
                case "decisión":
                    nodosDecision.add(node.getAttribute("id"));
                    break;

                case "bucle":

                    nodosNoDecision.add(node.getAttribute("id"));
                    idBucle = node.getAttribute("id");

                    ContainerNode nodoBuclePrimitivo = dibujarUnNodoBucle(bounds, node, nodeMap);
                    listaDeBucles.add(nodoBuclePrimitivo);

                    listasDeNodosParaBucles.add(new ArrayList<>());

                    for(int j = i + 1; !((Element) nodes.item(j)).getAttribute("tipo").equals("finBucle"+idBucle); ++j){

                        Element nodoInside = (Element) nodes.item(j);

                        String tipoEnBucle = nodoInside.getAttribute("tipo");

                        switch (tipoEnBucle){
                            case "bucle":
                                ContainerNode nodoContainer  = dibujarUnNodoBucle(bounds, nodoInside, nodeMap);
                                listasDeNodosParaBucles.get(enDonde).add(nodoContainer);
                                listasDeNodosParaBucles.add(new ArrayList<>());
                                ++enDonde;
                                ++cantidadTotalDeBucles;
                                listaDeBucles.add(nodoContainer);
                                nodosNoDecision.add(nodoInside.getAttribute("id"));
                                break;

                            case "proceso":
                                ShapeNode nodoProceso = dibujarUnNodo(bounds, nodoInside, nodeMap);
                                listasDeNodosParaBucles.get(enDonde).add(nodoProceso);
                                nodosNoDecision.add(nodoInside.getAttribute("id"));
                                break;

                            case "decisión":
                                ShapeNode nodoDecision = dibujarUnNodo(bounds, nodoInside, nodeMap);
                                listasDeNodosParaBucles.get(enDonde).add(nodoDecision);
                                nodosDecision.add(nodoInside.getAttribute("id"));
                                break;

                            default:
                                --enDonde;
                                break;
                        }

                        i=j;

                    }

                    ++cantidadTotalDeBucles;
                    enDonde = cantidadTotalDeBucles;

                    break;

                default:
                    if (!node.getAttribute("tipo").matches(".*\\d+.*")) {
                        nodosNoDecision.add(node.getAttribute("id"));
                    }
                    break;
            }

            if (!node.getAttribute("tipo").matches(".*\\d+.*") && !tipo.equals("bucle")) {
                dibujarUnNodo(bounds, node, nodeMap);
            }
        }
    }

    public ShapeNode dibujarUnNodo(RectF bounds, Element node, HashMap<String, DiagramNode> nodeMapFuncion){

        ShapeNode diagramNode = diagram.getFactory().createShapeNode(bounds);

        //Convierte el "tipo" ubicado en el xml en la forma
        manejador.conversor(node, diagramNode);
        String idNodo = node.getAttribute("id");
        nodeMapFuncion.put(idNodo, diagramNode);
        diagramNode.setText(node.getAttribute("nombre"));

        //Clave para que se vea bien el texto dentro del nodo
        diagramNode.resizeToFitText(FitSize.KeepRatio);

        return diagramNode;
    }

    public ContainerNode dibujarUnNodoBucle(RectF bounds, Element node,  HashMap<String, DiagramNode> nodeMapFuncion){

        ContainerNode bucle = diagram.getFactory().createContainerNode(bounds);
        bucle.setAutoShrink(true);

        //Convierte el "tipo" ubicado en el xml en la forma
        manejador.conversorNodoContainer(node, bucle);
        String idNodo = node.getAttribute("id");
        nodeMapFuncion.put(idNodo, bucle );
        bucle.setEditedText(node.getAttribute("nombre"));

        return bucle;

    }

    public void armarElLayout(Diagram diagrama){
        //Conn esto, menciono que si bien tome un layout de Decision, tambien tengo que mapear todas las relaciones de cada
        //uno de los nodos, es decir si hay uno que es decision, necesariamente tengo que crear los 2 links de decision seguidos,
        //no uno, y luego otro.
        LayeredLayout layout = new LayeredLayout();
        layout.setLayerDistance(10);
        layout.setNodeDistance(25);
        layout.setStraightenLongLinks(true);
        layout.arrange(diagrama);

        medidaDiagrama = diagrama.getContentBounds(false, true);
        diagrama.setBounds(medidaDiagrama);

    }

    public boolean esNodoDecision(String idNodo, List<String> nodosDecision) {
        boolean decision = false;
        if (nodosDecision.contains(idNodo)){
            decision = true;
        }
        return decision;
    }

    public List<String> obtenerNodosTargetsDadoUnNodoOrigenDeDecision(String idNodoDecision, NodeList links) {

        List<String> nodosTarget = new ArrayList<String>();
        for (int i = 0; i < links.getLength(); ++i){
            Element link = (Element)links.item(i);
            String idorigen = link.getAttribute("origin");
            if (idorigen.equals(idNodoDecision)) {
                nodosTarget.add(link.getAttribute("target"));
            }
        }
        return nodosTarget;
    }

    @Override
    public void onClick(View v) {
        File fdelete = new File(xmlName);
        switch (v.getId()){
            case R.id.guardar:
                startSave();
                fdelete.delete();
                finish();
                break;
            case R.id.descartar:
                fdelete.delete();
                finish();
                break;

        }
    }

    public void startSave(){
        FileOutputStream fileOutputStream = null;
        File file = getFilesDir();
        if(!file.exists() && !file.mkdirs()){
            Toast.makeText(this, "No se puede crear el directorio", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = titulo + "." + autor + ".png";
        String file_name = file.getAbsolutePath()+"/"+name;
        File new_file = new File(file_name);
        try{
            fileOutputStream = new FileOutputStream(new_file);
            Bitmap bitmap = diagram.createImage();
            bitmap.compress(Bitmap.CompressFormat.PNG,50,fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void progressBar(){
        AlertDialog.Builder mBuilderDelete = new AlertDialog.Builder(this);
        View mViewDelete = getLayoutInflater().inflate(R.layout.dialogprogreso, null);
        mBuilderDelete.setView(mViewDelete);
        dialog = mBuilderDelete.create();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

}