package com.dev.amazonadvisor;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.OvershootInterpolator;

import com.github.clans.fab.FloatingActionMenu;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class FollowingFragment extends Fragment {

    private FloatingActionMenu menuFab;
    private Handler uiHandler = new Handler();
    private RecyclerView recyclerView;
    private ListAdapter adapter;
    private RecyclerView.LayoutManager layoutManager;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View rootView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_following, container, false);
        recyclerView = (RecyclerView) rootView.findViewById(R.id.my_recycler_view);
        menuFab = (FloatingActionMenu) rootView.findViewById(R.id.menu_yellow);
        swipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swiperefresh);
        return rootView;
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if(dy <= -15)
                    menuFab.showMenuButton(true);
                else if(dy >= 15)
                    menuFab.hideMenuButton(true);
            }
        });
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }
            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                int position = viewHolder.getAdapterPosition();
                adapter.getDataset().remove(position);
                adapter.notifyItemRemoved(position);
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
        swipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        initiateAmazonService();
                    }
                }
        );
        menuFab.setClosedOnTouchOutside(true);
        menuFab.hideMenuButton(false);
        initiateAmazonService();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
            int delay = 400;
            uiHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    menuFab.showMenuButton(true);
                }
            }, delay);
            delay += 150;

            menuFab.setOnMenuButtonClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (menuFab.isOpened()) {
                        //do something
                    }

                    menuFab.toggle(true);
                }
            });

            createCustomAnimation();
    }


    private void createCustomAnimation() {

        AnimatorSet set = new AnimatorSet();

        ObjectAnimator scaleOutX = ObjectAnimator.ofFloat(menuFab.getMenuIconView(), "scaleX", 1.0f, 0.2f);
        ObjectAnimator scaleOutY = ObjectAnimator.ofFloat(menuFab.getMenuIconView(), "scaleY", 1.0f, 0.2f);

        ObjectAnimator scaleInX = ObjectAnimator.ofFloat(menuFab.getMenuIconView(), "scaleX", 0.2f, 1.0f);
        ObjectAnimator scaleInY = ObjectAnimator.ofFloat(menuFab.getMenuIconView(), "scaleY", 0.2f, 1.0f);

        scaleOutX.setDuration(50);
        scaleOutY.setDuration(50);

        scaleInX.setDuration(150);
        scaleInY.setDuration(150);

        scaleInX.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                Animation rotation = AnimationUtils.loadAnimation(getContext(), R.anim.button_rotation);
                rotation.setRepeatMode(Animation.RELATIVE_TO_SELF);
                menuFab.getMenuIconView().startAnimation(rotation);
                if(menuFab.isOpened())
                    menuFab.getMenuIconView().setRotation(0f);
                else
                    menuFab.getMenuIconView().setRotation(45f);
            }
        });

        set.play(scaleOutX).with(scaleOutY);
        set.play(scaleInX).with(scaleInY).after(scaleOutX);
        set.setInterpolator(new OvershootInterpolator(2));

        menuFab.setIconToggleAnimatorSet(set);
    }

    private void initiateAmazonService()
    {
        swipeRefreshLayout.setRefreshing(true);
        ArrayList<AmazonProduct> products = new ArrayList<>(AmazonProduct.listAll(AmazonProduct.class, "title"));
        if(products.size() > 0)
        {
            adapter = new ListAdapter(products, getActivity());
            recyclerView.setAdapter(adapter);
            swipeRefreshLayout.setRefreshing(false);
            return;
        }
        new DownloadProductsData().execute();
    }

    private class DownloadProductsData extends AsyncTask<Void, Void, ArrayList<AmazonProduct>>
    {

        private static final String AWS_ACCESS_KEY_ID = "AKIAIIZGFFBWXZXJSQNA";
        private static final String AWS_SECRET_KEY = "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";

        private final String ENDPOINT = getLocalizedAWSURL();

        private URL url;
        private HttpURLConnection conn;
        private BufferedReader reader;
        private ArrayList<String> productIDs = new ArrayList<>();
        private Element body;

        @Override
        protected ArrayList<AmazonProduct> doInBackground(Void... lists) {

            try
            {
                url = new URL("https://" + getLocalizedURL() + "/gp/registry/search");
                Map<String,Object> params = new LinkedHashMap<>();
                params.put("sortby", "");
                params.put("index", "it-xml-wishlist");
                params.put("field-name", getActivity().getSharedPreferences("ACCOUNT_INFO", Context.MODE_PRIVATE)
                                                      .getString("EMAIL", ""));
                params.put("field-firstname", "");
                params.put("field-lastname", "");
                params.put("nameOrEmail", "");
                params.put("submit.search", "");
                StringBuilder postData = new StringBuilder();
                for (Map.Entry<String,Object> param : params.entrySet()) {
                    if (postData.length() != 0) postData.append('&');
                    postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
                    postData.append('=');
                    postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
                }
                byte[] postDataBytes = postData.toString().getBytes("UTF-8");

                conn = (HttpURLConnection)url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
                conn.setDoOutput(true);
                conn.getOutputStream().write(postDataBytes);

                reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while((line = reader.readLine()) != null)
                    result.append(line);
                String finalResult = result.substring(result.indexOf("Advisor"), result.substring(result.indexOf("Advisor"))
                        .indexOf("<") + result.indexOf("Advisor"));
                String listAddress = finalResult.substring(finalResult.indexOf("href=\"")).replace("href=\"", "")
                        .replace("\">", "");
                System.out.println(listAddress + "\n\n\n");
                reader.close();


                boolean done = false;
                while(!done)
                    try
                    {
                        initConnection(listAddress);
                        done = true;
                    }
                    catch(IOException exc)
                    {
                        exc.printStackTrace();
                    }
                result = new StringBuilder();
                while((line = reader.readLine()) != null)
                    result.append(line);
                org.jsoup.nodes.Document doc = Jsoup.parseBodyFragment(result.toString());
                body = doc.body();
                Elements elements = body.getElementsByClass("a-spacing-large a-divider-normal");
                for(int i = 0; i < elements.size(); i++)
                {
                    result = new StringBuilder(result.substring(result.indexOf("<div id=\"item_")));
                    String temp = result.toString();
                    temp = temp.substring(temp.indexOf("asin="));
                    temp = temp.substring(0, temp.indexOf("&"));
                    temp = temp.substring(5);
                    productIDs.add(temp);
                    result = new StringBuilder(result.substring(16, result.length()));
                }
            }
            catch(MalformedURLException exc)
            {
                exc.printStackTrace();
            }
            catch(UnsupportedEncodingException exc)
            {
                exc.printStackTrace();
            }
            catch(IOException exc)
            {
                exc.printStackTrace();
            }

            ArrayList<AmazonProduct> products = new ArrayList<>();
            for(int i = 0; i < productIDs.size(); i++)
            {

                SignedRequestsHelper helper;
                try {
                    helper = SignedRequestsHelper.getInstance(ENDPOINT, AWS_ACCESS_KEY_ID, AWS_SECRET_KEY);
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }

                String requestUrl = null;

                Map<String, String> params = new HashMap<String, String>();
                params.put("Service", "AWSECommerceService");
                params.put("AssociateTag", "amazonadvis06-20");
                params.put("Version", "2009-03-31");
                params.put("Operation", "ItemLookup");
                params.put("ItemId", productIDs.get(i));
                params.put("ResponseGroup", "Large");
                requestUrl = helper.sign(params);

                AmazonProductContainer currentProduct = fetchProductData(requestUrl);
                AmazonProduct product = new AmazonProduct(productIDs.get(i), currentProduct.title, currentProduct.price,
                                                          currentProduct.seller, currentProduct.availability, "", currentProduct.prime,
                                                          ImageUtils.getByteArrayFromURL(currentProduct.mediumImagesURL),
                                                          currentProduct.rating);
                product.save();
                products.add(product);
            }
            return products;
        }

        @Override
        protected void onPostExecute(ArrayList<AmazonProduct> amazonProducts) {
            super.onPostExecute(amazonProducts);
            adapter = new ListAdapter(amazonProducts, getActivity());
            recyclerView.setAdapter(adapter);
            swipeRefreshLayout.setRefreshing(false);
        }

        private void initConnection(String listAddress) throws IOException
        {
            url = new URL("https://" + getLocalizedURL() + listAddress);
            conn = (HttpURLConnection)url.openConnection();
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        }

        private AmazonProductContainer fetchProductData(String requestUrl) {
            AmazonProductContainer container = new AmazonProductContainer();
            try
            {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document doc = null;
                int done = 0;
                while(done == 0)
                    try
                    {
                        doc = db.parse(requestUrl);
                        done = 1;
                    }
                    catch (FileNotFoundException exc)
                    {
                        exc.printStackTrace();
                    }
                Node title = doc.getElementsByTagName("Title").item(0);
                Node smallImageURL = doc.getElementsByTagName("SmallImage").item(0).getFirstChild();
                Node mediumImageURL = doc.getElementsByTagName("MediumImage").item(0).getFirstChild();
                Node largeImageURL = doc.getElementsByTagName("LargeImage").item(0).getFirstChild();
                Node manufacturer = doc.getElementsByTagName("Manufacturer").item(0);
                Node seller = doc.getElementsByTagName("Publisher").item(0);
                Node model = doc.getElementsByTagName("Model").item(0);
                Node price = doc.getElementsByTagName("ListPrice").item(0).getLastChild();
                Node itemsAsNew = doc.getElementsByTagName("TotalNew").item(0);
                Node itemsAsUsed = doc.getElementsByTagName("TotalUsed").item(0);
                Node hasPrime = doc.getElementsByTagName("IsEligibleForPrime").item(0);
                Node availability = doc.getElementsByTagName("Availability").item(0);
                NodeList features = doc.getElementsByTagName("Feature");
                NodeList itemDimension = doc.getElementsByTagName("ItemDimensions");
                container.title = title.getTextContent();
                container.smallImagesURL = smallImageURL.getTextContent();
                container.mediumImagesURL = mediumImageURL.getTextContent();
                container.largeImageURL = largeImageURL.getTextContent();
                container.manufacturer = manufacturer.getTextContent();
                container.seller = seller.getTextContent();
                container.model = model.getTextContent();
                container.price = price.getTextContent();
                container.itemsAsNew = Integer.parseInt(itemsAsNew.getTextContent());
                container.itemAsUsed = Integer.parseInt(itemsAsUsed.getTextContent());
                container.prime = hasPrime.getTextContent().equals("1");
                container.availability = availability.getTextContent();
                for(int i = 0; i < features.getLength(); i++)
                    container.features.add(features.item(i).getTextContent());
                for(int i = 0; i < itemDimension.item(0).getChildNodes().getLength(); i++)
                    container.itemDimension.add(Integer.parseInt(itemDimension.item(0).getChildNodes().item(i).getTextContent()));
                container.rating = getRatingFromASIN(doc.getElementsByTagName("Item").item(0).getFirstChild().getTextContent());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return container;
        }

        private String getRatingFromASIN(String ASIN) throws IOException
        {
            url = new URL("http://" + getLocalizedURL() + "/gp/customer-reviews/widgets/average-customer-review/popover/ref=dpx_acr_pop_?contextId=dpx&asin=" + ASIN);
            conn = (HttpURLConnection)url.openConnection();
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder result = new StringBuilder();
            String line;
            while((line = reader.readLine()) != null)
                result.append(line);
            org.jsoup.nodes.Document doc = Jsoup.parseBodyFragment(result.toString());
            Element body = doc.body();
            return body.getElementsByClass("a-size-base a-color-secondary").html();
        }

        private Locale getLocale()
        {
            return getActivity().getResources().getConfiguration().locale;
        }

        private String getLocalizedAWSURL()
        {
            Locale locale = getLocale();

            if(locale.equals(Locale.ITALY))
                return "webservices.amazon.it";
            if(locale.equals(Locale.US))
                return "ecs.amazonaws.com";
            if(locale.equals(Locale.CANADA))
                return "ecs.amazonaws.ca";
            if(locale.equals(Locale.UK))
                return "ecs.amazonaws.co.uk";
            if(locale.equals(Locale.FRANCE))
                return "ecs.amazonaws.fr";
            if(locale.equals(Locale.GERMANY))
                return "ecs.amazonaws.de";
            if(locale.equals(Locale.JAPAN))
                return "ecs.amazonaws.jp";
            return "ecs.amazonaws.com";
        }

        private String getLocalizedURL()
        {
            Locale locale = getLocale();

            if(locale.equals(Locale.ITALY))
                return "www.amazon.it";
            if(locale.equals(Locale.US))
                return "www.amazon.com";
            if(locale.equals(Locale.CANADA))
                return "www.amazon.ca";
            if(locale.equals(Locale.UK))
                return "www.amazon.co.uk";
            if(locale.equals(Locale.FRANCE))
                return "www.amazon.fr";
            if(locale.equals(Locale.GERMANY))
                return "www.amazon.de";
            if(locale.equals(Locale.JAPAN))
                return "www.amazon.jp";
            return "www.amazon.com";
        }
    }
}
