package org.dbweb.Arcomem.Integration;



import org.dbweb.Arcomem.datastructures.BasicSearchResult;
import org.dbweb.Arcomem.datastructures.CrawlID;
import org.dbweb.Arcomem.datastructures.GeneralConfigFile;
import org.dbweb.Arcomem.datastructures.Keywords;
import org.dbweb.Arcomem.datastructures.Seeker;


public interface SocialsearchInterface {
	BasicSearchResult BasicSearch(GeneralConfigFile cfgFile, CrawlID crawlid, Keywords keywords);
	BasicSearchResult BasicSearch(GeneralConfigFile cfgFile, CrawlID crawlid, Keywords keywords, Seeker seek);

//	BasicSearchResult BasicSearch(CrawlID crawlid, Keywords keywords, ResultSize k);
	
//	ArrayList<ItemID> ComplexSearch(CrawlID crawlid, Keywords keywords);
//	ArrayList<ItemID> ComplexSearch(CrawlID crawlid, Keywords keywords, Integer k);

}
