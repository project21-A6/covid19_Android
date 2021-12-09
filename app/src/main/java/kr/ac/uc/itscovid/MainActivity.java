package kr.ac.uc.itscovid;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraAnimation;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.util.FusedLocationSource;
import com.naver.maps.map.widget.LocationButtonView;
import com.naver.maps.map.widget.ZoomControlView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MainActivity";

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;

    public static final int PERMISSION_REQUEST_CODE = 100;
    public static final String[] PERMISSION = {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.INTERNET
    };

    //Map
    private FusedLocationSource mLocationSource;
    private NaverMap mNavermap;
    private Geocoder geocoder;
    private String click_location = "";

    // 마커를 찍을 데이터
    private ArrayList<PlaceInfo> mPlaceInfoList;

    //server 연동 retrofit
    private ArrayList<Covid> arrayCovid = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button withCovid = findViewById(R.id.withCovid);

        //retrofit
        ListView listView = findViewById(R.id.listview);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Api.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        Api api = retrofit.create(Api.class);

        Call<List<Covid>> call = api.get_Covid();

        call.enqueue(new Callback<List<Covid>>() {
            @Override
            public void onResponse(Call<List<Covid>> call, Response<List<Covid>> response) {
                if (response.isSuccessful() && response.body() != null){
                    List<Covid> covids = response.body();
                    String[] strArray_covid_info = new String[covids.size()];
                    int idx = 0;

                    //데이터 출력 전 현재 데이터 갯수 확인
                    Log.d("covid_log", Integer.toString(covids.size()));

                    for (Covid c : covids){
                        //받아온 데이터 저장
                        arrayCovid.add(c);
                    }
                }
                else{
                    Toast.makeText(getApplicationContext(), "Failed to get data from the Server", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Covid>> call, Throwable t) {
                Log.d("covid-err", t.getMessage().toString());
                Toast.makeText(getApplicationContext(), t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        //지역선택 스피너
        Spinner localSpinner = findViewById(R.id.spinner);
        ArrayAdapter localAdapter = ArrayAdapter.createFromResource(this, R.array.local_array, android.R.layout.simple_spinner_item);

        //스피너 메뉴 클릭시 창
        localAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //스피너 어댑터
        localSpinner.setAdapter(localAdapter);

        //map 사용 권한
        //위치 반환 FusedLocationSource
        mLocationSource = new FusedLocationSource(this, PERMISSION_REQUEST_CODE);

        //map 객체 생성
        FragmentManager fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment) fm.findFragmentById(R.id.map);

        if(mapFragment == null){
            mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.map, mapFragment).commit();
        }
        //onMapReady 호출
        mapFragment.getMapAsync(this);

        //스피너 클릭 이벤트
        localSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ArrayList<String> selectedArr = new ArrayList<String>();

                for (Covid c : arrayCovid){
                    if (arrayCovid.get(position).getGubun() == c.getGubun()){//선택한 지역과 배열 지역 비교
                        Log.e(">>loglog",c.getGubun());
                        //두 지역의 값이 같으면 배열에 데이터 추가
                        selectedArr.add("지역 : "+c.getGubun()+"\n 날짜 : "+c.getStdDay()
                                +"\n 총 확진자 수 : "+c.getDefCnt()+"\n 일일 확진자 수 :"+c.getLocalOccCnt());
                    }
                }
                listView.setAdapter(
                        new ArrayAdapter<String>(
                                getApplicationContext(),
                                android.R.layout.simple_list_item_1,
                                selectedArr
                                //Strein 타입
                                //지역 비교로 동일한 데이터 불러옴옴
                        )
                );
                Toast.makeText(parent.getContext(), "선택된 지역은 "+parent.getItemAtPosition(position), Toast.LENGTH_SHORT).show();
                if(mNavermap!=null){
                    goLocation(parent.getItemAtPosition(position).toString());
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                localSpinner.setSelection(0);
            }
        });
    }

    //버튼 선택 시 코로나 안내 페이지로 이동하도록
    public void withCorona(View v){
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("http://152.70.245.49/covid_level.html"));
        startActivity(intent);
    }


    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        Log.d(TAG, "onMapReady");
        //NaverMap 객체에 위치 소스 지정
        this.mNavermap = naverMap;

        //map marker 지역별 표시
        Marker marker_seoul = new Marker();
        Marker marker_incheon = new Marker();
        Marker marker_daejeon = new Marker();
        Marker marker_ulsan = new Marker();
        Marker marker_busan = new Marker();
        Marker marker_daegu = new Marker();
        Marker marker_gwangju = new Marker();
        Marker marker_sejong = new Marker();
        Marker marker_jeju = new Marker();
        Marker marker_gyeonggi = new Marker();
        Marker marker_gangwon = new Marker();
        Marker marker_jeonnam = new Marker();
        Marker marker_jeonbuk = new Marker();
        Marker marker_chungnam = new Marker();
        Marker marker_chungbuk = new Marker();
        Marker marker_gyeongnam = new Marker();
        Marker marker_gyeongbuk = new Marker();


        //서울특별시
        marker_seoul.setPosition(new LatLng(37.566681000053926, 126.97818609545838));
        //인천광역시
        marker_incheon.setPosition(new LatLng(37.45589896202555, 126.70555511443919));
        //대전광역시
        marker_daejeon.setPosition(new LatLng(36.350417444470786, 127.38487276429184));
        //울산광역시
        marker_ulsan.setPosition(new LatLng(35.5391031013572, 129.31131419291634));
        //부산광역시
        marker_busan.setPosition(new LatLng(35.17959835781201, 129.07510501528745));
        //대구광역시
        marker_daegu.setPosition(new LatLng(35.87137811492913, 128.60184731798805));
        //광주광역시
        marker_gwangju.setPosition(new LatLng(35.16007616164431, 126.85154443541715));
        //세종특별자치시
        marker_sejong.setPosition(new LatLng(36.48008012003388, 127.28889354415286));
        //제주특별자치도
        marker_jeju.setPosition(new LatLng(33.48899806725678, 126.49841411505886));
        //경기도
        marker_gyeonggi.setPosition(new LatLng(37.27517678102399, 127.0095603539667));
        //강원도
        marker_gangwon.setPosition(new LatLng(37.885323640650824, 127.72982088410237));
        //전라남도
        marker_jeonnam.setPosition(new LatLng(34.81614124945017, 126.4628363059835));
        //전라북도
        marker_jeonbuk.setPosition(new LatLng(35.82026428516564, 127.1087269981093));
        //충청남도
        marker_chungnam.setPosition(new LatLng(36.66011611569228, 126.67243078835754));
        //충청북도
        marker_chungbuk.setPosition(new LatLng(36.63570224426944, 127.49139065618134));
        //경상남도
        marker_gyeongnam.setPosition(new LatLng(35.23827295454108, 128.69241945576738));
        //경상북도
        marker_gyeongbuk.setPosition(new LatLng(36.57612201593526, 128.50564068576335));

        marker_seoul.setMap(naverMap);
        marker_incheon.setMap(naverMap);
        marker_daejeon.setMap(naverMap);
        marker_ulsan.setMap(naverMap);
        marker_busan.setMap(naverMap);
        marker_daegu.setMap(naverMap);
        marker_gwangju.setMap(naverMap);
        marker_sejong.setMap(naverMap);
        marker_jeju.setMap(naverMap);
        marker_gangwon.setMap(naverMap);
        marker_jeonnam.setMap(naverMap);
        marker_jeonbuk.setMap(naverMap);
        marker_chungnam.setMap(naverMap);
        marker_chungbuk.setMap(naverMap);
        marker_gyeongnam.setMap(naverMap);
        marker_gyeongbuk.setMap(naverMap);

        //NaverMap 객체에 위치 소스 지정
        this.mNavermap = naverMap;
        naverMap.setLocationSource(mLocationSource);

        //위치 추적 모드 지정 -> 내 위치
        naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);

        //UI setting
        UiSettings uiSettings = mNavermap.getUiSettings();
        uiSettings.setZoomControlEnabled(false);
        uiSettings.setLocationButtonEnabled(false);

        ZoomControlView zoomControlView = findViewById(R.id.itsZoom);
        zoomControlView.setMap(mNavermap);

        LocationButtonView locationButtonView = findViewById(R.id.myLocation);
        locationButtonView.setMap(mNavermap);

//        //권한 확인
        //결과 -> onRequestPermissionResult
        ActivityCompat.requestPermissions(this, PERMISSION, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if(mLocationSource.onRequestPermissionsResult(requestCode, permissions, grantResults)){
            if(!mLocationSource.isActivated()){
                //권한 거부
                mNavermap.setLocationTrackingMode(LocationTrackingMode.None);
            }
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        //권한획득 여부 확인
        if (requestCode == PERMISSION_REQUEST_CODE){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                mNavermap.setLocationTrackingMode(LocationTrackingMode.Follow);
            }
        }
    }

    //스피너에서 선택된 지역으로 이동하도록..
    public void goLocation(String click_location){
        CameraUpdate cameraUpdate;
        switch (click_location){
            case "서울특별시":
                cameraUpdate = CameraUpdate.scrollAndZoomTo(
                        new  LatLng(37.566681000053926, 126.97818609545838),15)
                        .animate(CameraAnimation.Fly, 3000);
                mNavermap.moveCamera(cameraUpdate);
                break;
            case "인천광역시":
                cameraUpdate = CameraUpdate.scrollAndZoomTo(
                        new LatLng(37.45589896202555, 126.70555511443919),15)
                        .animate(CameraAnimation.Fly, 3000);
                mNavermap.moveCamera(cameraUpdate);
                break;
            case "대전광역시":
                cameraUpdate = CameraUpdate.scrollAndZoomTo(
                        new LatLng(36.350417444470786, 127.38487276429184),15)
                        .animate(CameraAnimation.Fly, 3000);
                mNavermap.moveCamera(cameraUpdate);
                break;
            case "울산광역시":
                cameraUpdate = CameraUpdate.scrollAndZoomTo(
                        new  LatLng(35.5391031013572, 129.31131419291634),15)
                        .animate(CameraAnimation.Fly, 3000);
                mNavermap.moveCamera(cameraUpdate);
                break;
            case "부산광역시":
                cameraUpdate = CameraUpdate.scrollAndZoomTo(
                        new  LatLng(35.17959835781201, 129.07510501528745),15)
                        .animate(CameraAnimation.Fly, 3000);
                mNavermap.moveCamera(cameraUpdate);
                break;
            case "대구광역시":
                cameraUpdate = CameraUpdate.scrollAndZoomTo(
                        new  LatLng(35.87137811492913, 128.60184731798805),15)
                        .animate(CameraAnimation.Fly, 3000);
                mNavermap.moveCamera(cameraUpdate);
                break;
            case "광주광역시":
                cameraUpdate = CameraUpdate.scrollAndZoomTo(
                        new LatLng(35.16007616164431, 126.85154443541715),15)
                        .animate(CameraAnimation.Fly, 3000);
                mNavermap.moveCamera(cameraUpdate);
                break;
            case "세종특별자치시":
                cameraUpdate = CameraUpdate.scrollAndZoomTo(
                        new LatLng(36.48008012003388, 127.28889354415286),15)
                        .animate(CameraAnimation.Fly, 3000);
                mNavermap.moveCamera(cameraUpdate);
                break;
            case "제주특별자치도":
                cameraUpdate = CameraUpdate.scrollAndZoomTo(
                        new LatLng(33.48899806725678, 126.49841411505886),15)
                        .animate(CameraAnimation.Fly, 3000);
                mNavermap.moveCamera(cameraUpdate);
                break;
            case "경기도":
                cameraUpdate = CameraUpdate.scrollAndZoomTo(
                        new LatLng(37.27517678102399, 127.0095603539667),15)
                        .animate(CameraAnimation.Fly, 3000);
                mNavermap.moveCamera(cameraUpdate);
                break;
            case "강원도":
                cameraUpdate = CameraUpdate.scrollAndZoomTo(
                        new LatLng(37.885323640650824, 127.72982088410237),15)
                        .animate(CameraAnimation.Fly, 3000);
                mNavermap.moveCamera(cameraUpdate);
                break;
            case "전라남도":
                cameraUpdate = CameraUpdate.scrollAndZoomTo(
                        new LatLng(34.81614124945017, 126.4628363059835),15)
                        .animate(CameraAnimation.Fly, 3000);
                mNavermap.moveCamera(cameraUpdate);
                break;
            case "전라북도":
                cameraUpdate = CameraUpdate.scrollAndZoomTo(
                        new LatLng(35.82026428516564, 127.1087269981093),15)
                        .animate(CameraAnimation.Fly, 3000);
                mNavermap.moveCamera(cameraUpdate);
                break;
            case "충청남도":
                cameraUpdate = CameraUpdate.scrollAndZoomTo(
                        new LatLng(36.66011611569228, 126.67243078835754),15)
                        .animate(CameraAnimation.Fly, 3000);
                mNavermap.moveCamera(cameraUpdate);
                break;
            case "충청북도":
                cameraUpdate = CameraUpdate.scrollAndZoomTo(
                        new LatLng(36.63570224426944, 127.49139065618134),15)
                        .animate(CameraAnimation.Fly, 3000);
                mNavermap.moveCamera(cameraUpdate);
                break;
            case "경상남도":
                cameraUpdate = CameraUpdate.scrollAndZoomTo(
                        new LatLng(35.23827295454108, 128.69241945576738),15)
                        .animate(CameraAnimation.Fly, 3000);
                mNavermap.moveCamera(cameraUpdate);
                break;
            case "경상북도":
                cameraUpdate = CameraUpdate.scrollAndZoomTo(
                        new LatLng(36.57612201593526, 128.50564068576335),15)
                        .animate(CameraAnimation.Fly, 3000);
                mNavermap.moveCamera(cameraUpdate);
                break;

            default:
                Toast.makeText(getApplicationContext(), "선택된 지역은 "+ click_location +" 입니다.", Toast.LENGTH_SHORT).show();
                break;
        }

    }

}
