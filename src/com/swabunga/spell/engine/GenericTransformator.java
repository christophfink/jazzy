package  com.swabunga.spell.engine;import java.io.*;import java.util.*;/** A Generic implementation of a transformator takes an aspell *  phonetics file and constructs some sort of transformationtable using *  the inner class Rule. * * @author Robert Gustavsson (robert@lindesign.se) */public class GenericTransformator implements Transformator{        public static final char        ALPHABET_START='[';    public static final char        ALPHABET_END=']';    public static final String      KEYWORD_ALPHBET="alphabet";    public static final String[]    IGNORED_KEYWORDS={"version",                                                      "followup",                                                      "collapse_result"};    public static final char    STARTMULTI='(';    public static final char    ENDMULTI=')';    private Object[]    ruleArray=null;    private String      alphabetString=null;    public GenericTransformator(File phonetic)throws IOException{        buildRules(new BufferedReader(new FileReader(phonetic)));    }    /**     * Takes out all single character replacements and put them in an      * char array. This array can later be used for adding or changing      * letters in getSuggestion().     *      * @return An array of chars with replacements characters.     */    public char[] getCodeReplaceList(){        char[]              replacements;        TransformationRule  rule;        Vector              tmp=new Vector();        if(ruleArray==null)            return null;        for(int i=0;i<ruleArray.length;i++){            rule=(TransformationRule)ruleArray[i];            if(rule.getReplaceExp().length()==1)                tmp.addElement(rule.getReplaceExp());        }        replacements=new char[tmp.size()];        for(int i=0;i<tmp.size();i++){            replacements[i]=((String)tmp.elementAt(i)).charAt(0);        }        return replacements;    }    /**     * Builds up an char array with the chars in the alphabet of the language     * as it was read from the alphabet tag in the phonetic file.     *      * @return An array of chars representing the alphabet or null if no alphabet was available.     */    public char[] getAlphaReplaceList(){        if(alphabetString!=null)            return alphabetString.toCharArray();        return null;    }    /**    * Returns the phonetic code of the word.    */    public String transform(String word) {               if(ruleArray==null)            return null;        TransformationRule rule;        StringBuffer str=new StringBuffer(word.toUpperCase());        int strLength=str.length();        int startPos=0, add=1;        while(startPos<strLength){            //System.out.println("StartPos:"+startPos);            add=1;            for(int i=0;i<ruleArray.length;i++){                //System.out.println("Testing rule#:"+i);                rule=(TransformationRule)ruleArray[i];                if(rule.startsWithExp() && startPos>0)                    continue;                if(startPos+rule.lengthOfMatch()>=strLength)                    continue;                if(rule.isMatching(str,startPos)){                    str.replace(startPos,startPos+rule.getTakeOut(),rule.getReplaceExp());                    add=rule.getReplaceExp().length();                    strLength-=rule.getTakeOut();                    strLength+=add;                    //System.out.println("Replacing with rule#:"+i+" add="+add);                    break;                }            }            startPos+=add;        }        return str.toString();    }    // Used to build up the transformastion table.    private void buildRules(BufferedReader in)throws IOException{        String read=null;        LinkedList ruleList=new LinkedList();        while((read=in.readLine())!=null){            buildRule(realTrimmer(read),ruleList);        }        ruleArray=ruleList.toArray();    }        // Here is where the real work of reading the phonetics file is done.   private void buildRule(String str, LinkedList ruleList){        if(str.length()<1)            return;        for(int i=0;i<IGNORED_KEYWORDS.length;i++){            if(str.startsWith(IGNORED_KEYWORDS[i]))                return;        }        // A different alphabet is used for this language, will be read into         // the alphabetString variable.        if(str.startsWith(KEYWORD_ALPHBET)){            int start=str.indexOf(ALPHABET_START);            int end=str.lastIndexOf(ALPHABET_END);            //System.out.println(start+"->"+end);            if(end!=-1 && start!=-1){                alphabetString=str.substring(++start,end);                //System.out.println(alphabetString);            }            return;        }        TransformationRule rule=null;        StringBuffer matchExp=new StringBuffer();        StringBuffer replaceExp=new StringBuffer();        boolean start=false, end=false;        int takeOutPart=0, matchLength=0;        boolean match=true, inMulti=false;        for(int i=0;i<str.length();i++){            if(Character.isWhitespace(str.charAt(i))){                match=false;            }else{                if(match){                    if (!isReservedChar(str.charAt(i))){                        matchExp.append(str.charAt(i));                        if(!inMulti){                            takeOutPart++;                            matchLength++;                        }                        if(str.charAt(i)==STARTMULTI || str.charAt(i)==ENDMULTI)                            inMulti=!inMulti;                    }                    if (str.charAt(i)=='-')                        takeOutPart--;                    if (str.charAt(i)=='^')                        start=true;                    if (str.charAt(i)=='$')                        end=true;                }else{                    replaceExp.append(str.charAt(i));                }            }        }        rule=new TransformationRule(matchExp.toString(), replaceExp.toString()                                        , takeOutPart, matchLength, start, end);        //System.out.println(rule.toString());        ruleList.add(rule);    }        // Chars with special meaning to aspell. Not everyone is implemented here.    private boolean isReservedChar(char ch){        if(ch=='<' || ch=='>' || ch=='^' || ch=='$' || ch=='-' || Character.isDigit(ch))            return true;        return false;    }    // Trims off everything we don't care about.    private String realTrimmer(String row){        int pos=row.indexOf('#');        if(pos!=-1){            row=row.substring(0,pos);        }        return row.trim();    }    // Inner Classes    /*    * Holds the match string and the replace string and all the rule attributes.    * Is responsible for indicating matches.    */    private class TransformationRule{        private String replace;        private char[] match;        // takeOut=number of chars to replace;         // matchLength=length of matching string counting multies as one.        private int takeOut, matchLength;        private boolean start, end;        // Construktor        public TransformationRule(String match, String replace, int takeout                                  , int matchLength, boolean start, boolean end){            this.match=match.toCharArray();            this.replace=replace;            this.takeOut=takeout;            this.matchLength=matchLength;            this.start=start;            this.end=end;        }        /*        * Returns true if word from pos and forward matches the match string.        * Precondition: wordPos+matchLength<word.length()        */        public boolean isMatching(StringBuffer word, int wordPos){            boolean matching=true, inMulti=false, multiMatch=false;            char matchCh;                        for(int matchPos=0;matchPos<match.length;matchPos++){                matchCh=match[matchPos];                if(matchCh==STARTMULTI || matchCh==ENDMULTI){                    inMulti=!inMulti;                    if(!inMulti)                        matching=matching & multiMatch;                    else                        multiMatch=false;                }else{                    if(matchCh!=word.charAt(wordPos)){                        if(inMulti)                            multiMatch=multiMatch | false;                        else                            matching=false;                    }else{                        if(inMulti)                            multiMatch=multiMatch | true;                        else                            matching=true;                    }                    if(!inMulti)                        wordPos++;                    if(!matching)                        break;                }            }            if(end && wordPos!=word.length()-1)                matching=false;            return matching;        }        public String getReplaceExp(){            return  replace;        }        public int getTakeOut(){            return takeOut;        }        public boolean startsWithExp(){            return start;        }                public int lengthOfMatch(){            return matchLength;        }              // Just for debugging purposes.        public String toString(){            return "Match:"+String.valueOf(match)                   +" Replace:"+replace                   +" TakeOut:"+takeOut                   +" MatchLength:"+matchLength                   +" Start:"+start                   +" End:"+end;        }    }}