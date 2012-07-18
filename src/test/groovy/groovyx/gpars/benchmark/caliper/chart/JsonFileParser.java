// GPars - Groovy Parallel Systems
//
// Copyright © 2008-2012  The original author or authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package groovyx.gpars.benchmark.caliper.chart;

import com.google.caliper.model.Run;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: don
 * Date: 7/16/12
 * Time: 1:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class JsonFileParser {

    JsonObject jsonObject;

    public JsonFileParser(File file){
        parseFile(file);
    }

    private void parseFile(File file){
        byte[] buffer = new byte[(int) file.length()];
        DataInputStream in = null;
        String result = null;
        try {
            in = new DataInputStream(new FileInputStream(file));
            in.readFully(buffer);
            result = new String(buffer);
        } catch (Exception e) {
            e.printStackTrace();
        }finally{
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


       JsonParser parser = new JsonParser();
        try{
            jsonObject = (JsonObject)parser.parse(result);
        }catch(ClassCastException e){
            jsonObject = new JsonObject();
        }
    }

    public ArrayList<Integer> getMeasurements(){
        JsonArray jsonArray = jsonObject.getAsJsonArray("results");
        ArrayList<Integer> medianList = new ArrayList<Integer>();
        if(jsonArray == null) return medianList;

        for(int scenario=0; scenario < jsonArray.size(); scenario++){
            JsonArray measurementArray = jsonArray.get(scenario).getAsJsonObject().getAsJsonArray("measurements");
            ArrayList<Integer> trials = new ArrayList<Integer>();

            for(int trial=0; trial < measurementArray.size(); trial++){
                JsonObject measurement = measurementArray.get(trial).getAsJsonObject();
                trials.add(measurement.get("value").getAsInt() / measurement.get("weight").getAsInt());
            }
            medianList.add(trials.get(trials.size() / 2));
        }
        return medianList;
    }


    public ArrayList<String> getScenarios(){
        JsonArray jsonArray = jsonObject.getAsJsonArray("scenarios");
        ArrayList<String> result = new ArrayList<String>();
        if(jsonArray == null) return result;
        for(int scenario=0; scenario < jsonArray.size(); scenario++){
            result.add(jsonArray.get(scenario).getAsJsonObject().get("userParameters").getAsJsonObject().get("numberOfClients").getAsString());
        }
        return result;
    }
}
