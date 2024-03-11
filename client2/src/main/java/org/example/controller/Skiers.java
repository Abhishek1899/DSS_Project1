package org.example.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

import com.google.gson.Gson;




@WebServlet(name = "Skiers", value = "Skiers")
public class Skiers extends HttpServlet {
    public class SkierLiftRideEvent{
        public int skierID;
		public int resortID;
		public int liftID;
		public int seasonID;
		public int dayID;
		public int time;

    private void processRequest(HttpServletResponse response) throws ServletException, IOException{
        PrintWriter out=response.getWriter();

        //############ TESTING RETRY ################//
        
        Random rand=new Random();
		int range=rand.nextInt(0,10);
        if (range < 5){
            response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
        }

        ////////////////////////////////////////////////////
        if (
            !(skierID >= 1 && skierID < 100000) ||
            !(resortID >= 1 && resortID < 10) ||
            !(liftID >= 1 && liftID < 40) ||
            !(seasonID == 2022) ||
            !(dayID == 1) ||
            !(time >= 1 && time < 360)
        ) 
        {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("Failed!, Please Check Parameters...");
    
        }
        //############### Set Resoponse #################/
        
        else{
            out.write("Post Success!!!");
        }

    }

        
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
       
        response.getOutputStream().print("Success!!!");
     
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");        
        Gson gson = new Gson();
        
        StringBuilder sb = new StringBuilder();
            String s;
            while ((s = request.getReader().readLine()) != null) {
                sb.append(s);
            }
            SkierLiftRideEvent se=(SkierLiftRideEvent)gson.fromJson(sb.toString(), SkierLiftRideEvent.class);
            se.processRequest(response);


            
        }
 
}
