package kr.ac.uc.itscovid;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.POST;

public interface Api {

    //서버 주소
    String BASE_URL = "http://152.70.245.49/" ;

    //반환 type : list
    @POST("data.php")
    Call<List<Covid>> get_Covid();
}
