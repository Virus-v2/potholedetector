package org.reapbenefit.gautam.intern.potholedetectorbeta.Fragments;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;

import org.reapbenefit.gautam.intern.potholedetectorbeta.TripListAdapter;
import org.reapbenefit.gautam.intern.potholedetectorbeta.R;

import java.io.File;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link TriplistFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link TriplistFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TriplistFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    File dir;
    File[] logfiles;
    ListView l;
    ImageButton refreshButton;

    public TriplistFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment TriplistFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static TriplistFragment newInstance(String param1, String param2) {
        TriplistFragment fragment = new TriplistFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        getListofLogFiles();
        int n;
        if(logfiles != null)
            n = logfiles.length;
        else n =0;
        for(int i=0; i<n; i++){
            Log.i("File "+String.valueOf(i), logfiles[i].getName());

        }
    }

    private void getListofLogFiles(){
        dir = getContext().getFilesDir();
        String path = dir.getPath() + "logs/";
        File logdir = new File(path);
       // file = new File(MainActivity.this.getFilesDir(), t2);
        logfiles = logdir.listFiles();
    }

    private void createListView(){
        if(logfiles!=null) {
            l.setAdapter(new TripListAdapter(getActivity(), logfiles));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS + "/PotholeApp/");
        logfiles = dir.listFiles();
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_triplist, container, false);
        refreshButton = (ImageButton) v.findViewById(R.id.refreshButton);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getListofLogFiles();
                createListView();
            }
        });


        l = (ListView) v.findViewById(R.id.trips_list);
        createListView();
        l.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                /*   *********  Opens a map activity, DUMMY
                Intent intent = new Intent(getActivity(), MapsActivity.class);
                Bundle b = new Bundle();
                b.putSerializable("file", logfiles[i]);
                intent.putExtra("file", logfiles[i]);
                startActivity(intent);
                */
                // Start a map activity that plots the locations

                // Upload the related file to google drive
               /*
                String your_file_path = logfiles[i].getPath();
                Intent intent = new Intent(android.content.Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + your_file_path));
                startActivity(Intent.createChooser(intent, ""));
                */

                // Open a detailed activity with a maps fragment
            }
        });
        return v;
    }

    /*
    public void saveToPrefs(Float h, float l, String car, String model){
        SharedPreferences.Editor editor = getActivity().getSharedPreferences("Profiles", MODE_PRIVATE).edit();
        editor.clear();
        editor.putString("CurrentCar", car);
        editor.putString("CurrentModel", model);
        editor.putFloat("High", h);
        editor.putFloat("Low", l);
        editor.commit();
    }
    */

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}