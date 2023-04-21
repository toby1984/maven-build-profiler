package de.codesourcery.maven.buildprofiler.shared;

public class Utils
{
    public static String jsonString(String input)
    {
        if ( input == null )
        {
            return "null";
        }
        final char[] array = input.toCharArray();
        final StringBuilder result = new StringBuilder(array.length);
        for ( char c : array )
        {
            switch( c )
            {
                case '\\' -> result.append( "\\\\" );
                case '"' -> result.append( "\\\"" );
                case '\f' -> result.append( "\\f" );
                case '\b' -> result.append( "\\b" );
                case '\t' -> result.append( "\\t" );
                case '\r' -> result.append( "\\r" );
                case '\n' -> result.append( "\\n" );
                default -> result.append( c );
            }
        }
        return '"' + result.toString() + '"';
    }
}
