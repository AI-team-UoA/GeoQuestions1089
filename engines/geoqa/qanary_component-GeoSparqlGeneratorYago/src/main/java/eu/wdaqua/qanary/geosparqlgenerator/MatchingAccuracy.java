package eu.wdaqua.qanary.geosparqlgenerator;

public class MatchingAccuracy {

    public boolean containsAsk(String query){
        if(query.contains("ASK"))
            return true;
        return false;
    }

    public boolean containsGroupBy(String query){
        if(query.contains("group by"))
            return true;
        return false;
    }

    public boolean containsOrderBy(String query){
        if(query.contains("order by"))
            return true;
        return false;
    }

    public boolean containsAggregates(String query){
        // need to update the code
        if(query.contains("limit"))
            return true;
        return false;
    }

    public boolean containsSpatialFilter(String query){
        //need to update the code
        if(query.contains("limit"))
            return true;
        return false;
    }

    public boolean containsLimit(String query){
        if(query.contains("limit"))
            return true;
        return false;
    }

    public boolean containsPredicate(String query){
        //need to update the code
        if(query.contains("limit"))
            return true;
        return false;
    }

    public static void main(String s[]){

        double askOrSelectAccuracy = 0.0;
        double groupByAccuracy = 0.0;
        double orderByAccuracy = 0.0;
        double limitAccuracy = 0.0;
        double aggregatesAccuracy = 0.0;
        double saptialFilterAccuracy = 0.0;
        double entityAccuracy = 0.0;
        double classAccuracy = 0.0;
        double predicateAccuracy = 0.0;

        boolean askOrSelectFlag = false;
        boolean groupByFlag = false;
        boolean orderByFlag = false;
        boolean limitFlag = false;
        boolean aggregatesFlag = false;
        boolean saptialFilterFlag = false;
        boolean entityFlag = false;
        boolean classFlag = false;
        boolean predicateFlag = false;

        String goldQuery = "";
        String spatialFilter = "";

    }


}
