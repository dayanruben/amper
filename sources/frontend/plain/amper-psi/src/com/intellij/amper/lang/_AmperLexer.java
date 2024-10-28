// Generated by JFlex 1.9.2 http://jflex.de/  (tweaked for IntelliJ platform)
// source: _AmperLexer.flex

package com.intellij.amper.lang;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

import static com.intellij.amper.lang.AmperElementTypes.*;
import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;


public class _AmperLexer implements FlexLexer {

  /** This character denotes the end of file */
  public static final int YYEOF = -1;

  /** initial size of the lookahead buffer */
  private static final int ZZ_BUFFERSIZE = 16384;

  /** lexical states */
  public static final int YYINITIAL = 0;

  /**
   * ZZ_LEXSTATE[l] is the state in the DFA for the lexical state l
   * ZZ_LEXSTATE[l+1] is the state in the DFA for the lexical state l
   *                  at the beginning of a line
   * l is of the form l = 2*k, k a non negative integer
   */
  private static final int ZZ_LEXSTATE[] = {
     0, 0
  };

  /**
   * Top-level table for translating characters to character classes
   */
  private static final int [] ZZ_CMAP_TOP = zzUnpackcmap_top();

  private static final String ZZ_CMAP_TOP_PACKED_0 =
    "\1\0\1\u0100\1\u0200\1\u0300\1\u0400\1\u0500\1\u0600\1\u0700"+
    "\1\u0800\1\u0900\1\u0a00\1\u0b00\1\u0c00\1\u0d00\1\u0e00\1\u0f00"+
    "\1\u1000\1\u0100\1\u1100\1\u1200\1\u1300\1\u0100\1\u1400\1\u1500"+
    "\1\u1600\1\u1700\1\u1800\1\u1900\1\u1a00\1\u1b00\1\u0100\1\u1c00"+
    "\1\u1d00\1\u1e00\12\u1f00\1\u2000\1\u2100\1\u2200\1\u1f00\1\u2300"+
    "\1\u2400\2\u1f00\31\u0100\1\u2500\121\u0100\1\u2600\4\u0100\1\u2700"+
    "\1\u0100\1\u2800\1\u2900\1\u2a00\1\u2b00\1\u2c00\1\u2d00\53\u0100"+
    "\1\u2e00\10\u2f00\31\u1f00\1\u0100\1\u3000\1\u3100\1\u0100\1\u3200"+
    "\1\u3300\1\u3400\1\u3500\1\u3600\1\u3700\1\u3800\1\u3900\1\u3a00"+
    "\1\u0100\1\u3b00\1\u3c00\1\u3d00\1\u3e00\1\u3f00\1\u4000\1\u4100"+
    "\1\u4200\1\u4300\1\u4400\1\u4500\1\u4600\1\u4700\1\u4800\1\u4900"+
    "\1\u4a00\1\u4b00\1\u4c00\1\u4d00\1\u4e00\1\u1f00\1\u4f00\1\u5000"+
    "\1\u5100\1\u5200\3\u0100\1\u5300\1\u5400\1\u5500\12\u1f00\4\u0100"+
    "\1\u5600\17\u1f00\2\u0100\1\u5700\41\u1f00\2\u0100\1\u5800\1\u5900"+
    "\2\u1f00\1\u5a00\1\u5b00\27\u0100\1\u5c00\4\u0100\1\u5d00\1\u5e00"+
    "\42\u1f00\1\u0100\1\u5f00\1\u6000\11\u1f00\1\u6100\24\u1f00\1\u6200"+
    "\1\u6300\1\u1f00\1\u6400\1\u6500\1\u6600\1\u6700\2\u1f00\1\u6800"+
    "\5\u1f00\1\u6900\1\u6a00\1\u6b00\5\u1f00\1\u6c00\1\u6d00\2\u1f00"+
    "\1\u6e00\1\u1f00\1\u6f00\14\u1f00\1\u7000\4\u1f00\246\u0100\1\u7100"+
    "\20\u0100\1\u7200\1\u7300\25\u0100\1\u7400\34\u0100\1\u7500\14\u1f00"+
    "\2\u0100\1\u7600\5\u1f00\23\u0100\1\u7700\u0aec\u1f00\1\u7800\1\u7900"+
    "\u02fe\u1f00";

  private static int [] zzUnpackcmap_top() {
    int [] result = new int[4352];
    int offset = 0;
    offset = zzUnpackcmap_top(ZZ_CMAP_TOP_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackcmap_top(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }


  /**
   * Second-level tables for translating characters to character classes
   */
  private static final int [] ZZ_CMAP_BLOCKS = zzUnpackcmap_blocks();

  private static final String ZZ_CMAP_BLOCKS_PACKED_0 =
    "\11\0\1\1\1\2\2\3\1\2\16\0\4\4\1\1"+
    "\1\5\1\6\1\7\1\0\2\4\1\10\1\11\1\12"+
    "\1\13\1\14\1\15\1\16\1\17\1\20\1\21\11\22"+
    "\1\23\2\4\1\24\2\4\1\25\4\0\1\26\6\0"+
    "\1\27\16\0\1\30\1\31\1\32\1\4\1\33\1\4"+
    "\1\34\3\0\1\35\1\36\5\0\1\37\1\0\1\40"+
    "\3\0\1\41\1\42\1\43\1\44\1\45\4\0\1\46"+
    "\1\4\1\47\1\4\6\0\1\50\32\0\1\1\1\4"+
    "\4\0\4\4\1\0\2\4\1\0\7\4\1\0\4\4"+
    "\1\0\5\4\27\0\1\4\37\0\1\4\u01ca\0\4\4"+
    "\14\0\16\4\5\0\7\4\1\0\1\4\1\0\21\4"+
    "\165\0\1\4\2\0\2\4\4\0\1\4\1\0\6\4"+
    "\1\0\1\4\3\0\1\4\1\0\1\4\24\0\1\4"+
    "\123\0\1\4\213\0\1\4\5\0\2\4\246\0\1\4"+
    "\46\0\2\4\1\0\6\4\51\0\6\4\1\0\1\4"+
    "\55\0\1\4\1\0\1\4\2\0\1\4\2\0\1\4"+
    "\1\0\10\4\33\0\4\4\4\0\15\4\6\0\5\4"+
    "\1\0\4\4\13\0\1\4\1\0\3\4\112\0\4\4"+
    "\146\0\1\4\11\0\1\4\12\0\1\4\23\0\2\4"+
    "\1\0\17\4\74\0\2\4\145\0\16\4\66\0\4\4"+
    "\1\0\2\4\61\0\22\4\34\0\4\4\13\0\65\4"+
    "\25\0\1\4\22\0\13\4\221\0\2\4\12\0\1\4"+
    "\23\0\1\4\10\0\2\4\2\0\2\4\26\0\1\4"+
    "\7\0\1\4\1\0\3\4\4\0\2\4\11\0\2\4"+
    "\2\0\2\4\4\0\10\4\1\0\4\4\2\0\1\4"+
    "\5\0\2\4\16\0\7\4\2\0\1\4\1\0\2\4"+
    "\3\0\1\4\6\0\4\4\2\0\2\4\26\0\1\4"+
    "\7\0\1\4\2\0\1\4\2\0\1\4\2\0\2\4"+
    "\1\0\1\4\5\0\4\4\2\0\2\4\3\0\3\4"+
    "\1\0\7\4\4\0\1\4\1\0\7\4\20\0\13\4"+
    "\3\0\1\4\11\0\1\4\3\0\1\4\26\0\1\4"+
    "\7\0\1\4\2\0\1\4\5\0\2\4\12\0\1\4"+
    "\3\0\1\4\3\0\2\4\1\0\17\4\4\0\2\4"+
    "\12\0\1\4\1\0\7\4\7\0\1\4\3\0\1\4"+
    "\10\0\2\4\2\0\2\4\26\0\1\4\7\0\1\4"+
    "\2\0\1\4\5\0\2\4\11\0\2\4\2\0\2\4"+
    "\3\0\7\4\3\0\4\4\2\0\1\4\5\0\2\4"+
    "\12\0\1\4\1\0\20\4\2\0\1\4\6\0\3\4"+
    "\3\0\1\4\4\0\3\4\2\0\1\4\1\0\1\4"+
    "\2\0\3\4\2\0\3\4\3\0\3\4\14\0\4\4"+
    "\5\0\3\4\3\0\1\4\4\0\2\4\1\0\6\4"+
    "\1\0\16\4\12\0\11\4\1\0\6\4\15\0\1\4"+
    "\3\0\1\4\27\0\1\4\20\0\3\4\10\0\1\4"+
    "\3\0\1\4\4\0\7\4\2\0\1\4\3\0\5\4"+
    "\4\0\2\4\12\0\20\4\4\0\1\4\10\0\1\4"+
    "\3\0\1\4\27\0\1\4\12\0\1\4\5\0\2\4"+
    "\11\0\1\4\3\0\1\4\4\0\7\4\2\0\7\4"+
    "\1\0\1\4\4\0\2\4\12\0\1\4\2\0\15\4"+
    "\15\0\1\4\3\0\1\4\63\0\1\4\3\0\1\4"+
    "\5\0\5\4\4\0\7\4\5\0\2\4\12\0\12\4"+
    "\6\0\1\4\3\0\1\4\22\0\3\4\30\0\1\4"+
    "\11\0\1\4\1\0\2\4\7\0\3\4\1\0\4\4"+
    "\6\0\1\4\1\0\1\4\10\0\6\4\12\0\2\4"+
    "\2\0\15\4\72\0\4\4\20\0\1\4\12\0\47\4"+
    "\2\0\1\4\1\0\1\4\5\0\1\4\30\0\1\4"+
    "\1\0\1\4\27\0\2\4\5\0\1\4\1\0\1\4"+
    "\6\0\2\4\12\0\2\4\4\0\40\4\1\0\27\4"+
    "\2\0\6\4\12\0\13\4\1\0\1\4\1\0\1\4"+
    "\1\0\4\4\12\0\1\4\44\0\4\4\24\0\1\4"+
    "\22\0\1\4\44\0\11\4\1\0\71\4\112\0\6\4"+
    "\116\0\2\4\46\0\1\4\1\0\5\4\1\0\2\4"+
    "\53\0\1\4\115\0\1\4\4\0\2\4\7\0\1\4"+
    "\1\0\1\4\4\0\2\4\51\0\1\4\4\0\2\4"+
    "\41\0\1\4\4\0\2\4\7\0\1\4\1\0\1\4"+
    "\4\0\2\4\17\0\1\4\71\0\1\4\4\0\2\4"+
    "\103\0\2\4\3\0\40\4\20\0\20\4\126\0\2\4"+
    "\6\0\3\4\u016c\0\2\4\21\0\1\1\32\0\5\4"+
    "\113\0\3\4\13\0\7\4\15\0\1\4\7\0\13\4"+
    "\25\0\13\4\24\0\14\4\15\0\1\4\3\0\1\4"+
    "\2\0\14\4\124\0\3\4\1\0\3\4\3\0\2\4"+
    "\12\0\41\4\4\0\1\4\12\0\6\4\131\0\7\4"+
    "\53\0\5\4\106\0\12\4\37\0\1\4\14\0\4\4"+
    "\14\0\12\4\50\0\2\4\5\0\13\4\54\0\4\4"+
    "\32\0\6\4\12\0\46\4\34\0\4\4\77\0\1\4"+
    "\35\0\2\4\13\0\6\4\12\0\15\4\1\0\10\4"+
    "\16\0\1\4\2\0\77\4\114\0\4\4\12\0\21\4"+
    "\11\0\14\4\164\0\14\4\70\0\10\4\12\0\3\4"+
    "\61\0\2\4\11\0\7\4\53\0\2\4\3\0\20\4"+
    "\3\0\1\4\47\0\5\4\372\0\1\4\33\0\2\4"+
    "\6\0\2\4\46\0\2\4\6\0\2\4\10\0\1\4"+
    "\1\0\1\4\1\0\1\4\1\0\1\4\37\0\2\4"+
    "\65\0\1\4\7\0\1\4\1\0\3\4\3\0\1\4"+
    "\7\0\3\4\4\0\2\4\6\0\4\4\15\0\5\4"+
    "\3\0\1\4\7\0\3\4\13\1\5\0\30\4\2\3"+
    "\5\0\1\1\17\4\2\0\23\4\1\0\12\4\1\1"+
    "\5\0\1\4\12\0\1\4\1\0\15\4\1\0\20\4"+
    "\15\0\3\4\40\0\20\4\15\0\4\4\1\0\3\4"+
    "\14\0\21\4\1\0\4\4\1\0\2\4\12\0\1\4"+
    "\1\0\3\4\5\0\6\4\1\0\1\4\1\0\1\4"+
    "\1\0\1\4\4\0\1\4\13\0\2\4\4\0\5\4"+
    "\5\0\4\4\1\0\21\4\51\0\u0177\4\57\0\1\4"+
    "\57\0\1\4\205\0\6\4\11\0\14\4\46\0\1\4"+
    "\1\0\5\4\1\0\2\4\70\0\7\4\1\0\17\4"+
    "\30\0\11\4\7\0\1\4\7\0\1\4\7\0\1\4"+
    "\7\0\1\4\7\0\1\4\7\0\1\4\7\0\1\4"+
    "\7\0\1\4\40\0\57\4\1\0\320\4\1\1\4\4"+
    "\3\0\31\4\17\0\1\4\5\0\2\4\5\0\4\4"+
    "\126\0\2\4\2\0\2\4\3\0\1\4\132\0\1\4"+
    "\4\0\5\4\53\0\1\4\136\0\21\4\40\0\60\4"+
    "\320\0\100\4\375\0\3\4\215\0\103\4\56\0\2\4"+
    "\15\0\3\4\34\0\24\4\60\0\4\4\12\0\1\4"+
    "\163\0\45\4\11\0\2\4\147\0\2\4\65\0\2\4"+
    "\11\0\52\4\63\0\4\4\1\0\13\4\1\0\7\4"+
    "\64\0\14\4\106\0\12\4\12\0\6\4\30\0\3\4"+
    "\1\0\1\4\61\0\2\4\44\0\14\4\35\0\3\4"+
    "\101\0\16\4\13\0\6\4\37\0\1\4\67\0\11\4"+
    "\16\0\2\4\12\0\6\4\27\0\3\4\111\0\30\4"+
    "\3\0\2\4\20\0\2\4\5\0\12\4\6\0\2\4"+
    "\6\0\2\4\6\0\11\4\7\0\1\4\7\0\1\4"+
    "\53\0\1\4\16\0\6\4\173\0\1\4\2\0\2\4"+
    "\12\0\6\4\244\0\14\4\27\0\4\4\61\0\4\4"+
    "\u0100\51\156\0\2\4\152\0\46\4\7\0\14\4\5\0"+
    "\5\4\14\0\1\4\15\0\1\4\5\0\1\4\1\0"+
    "\1\4\2\0\1\4\2\0\1\4\154\0\41\4\153\0"+
    "\22\4\100\0\2\4\66\0\50\4\15\0\3\4\20\0"+
    "\20\4\20\0\3\4\2\0\30\4\3\0\31\4\1\0"+
    "\6\4\5\0\1\4\207\0\2\4\1\0\4\4\1\0"+
    "\13\4\12\0\7\4\32\0\4\4\1\0\1\4\32\0"+
    "\13\4\131\0\3\4\6\0\2\4\6\0\2\4\6\0"+
    "\2\4\3\0\3\4\2\0\3\4\2\0\22\4\3\0"+
    "\4\4\14\0\1\4\32\0\1\4\23\0\1\4\2\0"+
    "\1\4\17\0\2\4\16\0\42\4\173\0\105\4\65\0"+
    "\210\4\1\0\202\4\35\0\3\4\61\0\17\4\1\0"+
    "\37\4\40\0\15\4\36\0\5\4\53\0\5\4\36\0"+
    "\2\4\44\0\4\4\10\0\1\4\5\0\52\4\236\0"+
    "\2\4\12\0\6\4\44\0\4\4\44\0\4\4\50\0"+
    "\10\4\64\0\234\4\67\0\11\4\26\0\12\4\10\0"+
    "\230\4\6\0\2\4\1\0\1\4\54\0\1\4\2\0"+
    "\3\4\1\0\2\4\27\0\12\4\27\0\11\4\37\0"+
    "\101\4\23\0\1\4\2\0\12\4\26\0\12\4\32\0"+
    "\106\4\70\0\6\4\2\0\100\4\4\0\1\4\2\0"+
    "\5\4\10\0\1\4\3\0\1\4\35\0\2\4\3\0"+
    "\4\4\1\0\40\4\35\0\3\4\35\0\43\4\10\0"+
    "\1\4\36\0\31\4\66\0\12\4\26\0\12\4\23\0"+
    "\15\4\22\0\156\4\111\0\67\4\63\0\15\4\63\0"+
    "\15\4\50\0\10\4\12\0\u0146\4\52\0\1\4\2\0"+
    "\3\4\2\0\116\4\35\0\12\4\1\0\10\4\41\0"+
    "\137\4\25\0\33\4\27\0\11\4\107\0\37\4\12\0"+
    "\17\4\74\0\2\4\1\0\17\4\1\0\2\4\31\0"+
    "\7\4\12\0\6\4\65\0\1\4\12\0\4\4\4\0"+
    "\10\4\44\0\2\4\1\0\11\4\105\0\4\4\4\0"+
    "\1\4\15\0\1\4\1\0\43\4\22\0\1\4\45\0"+
    "\6\4\1\0\101\4\7\0\1\4\1\0\1\4\4\0"+
    "\1\4\17\0\1\4\12\0\7\4\73\0\5\4\12\0"+
    "\6\4\4\0\1\4\10\0\2\4\2\0\2\4\26\0"+
    "\1\4\7\0\1\4\2\0\1\4\5\0\1\4\12\0"+
    "\2\4\2\0\2\4\3\0\2\4\1\0\6\4\1\0"+
    "\5\4\7\0\2\4\7\0\3\4\5\0\213\4\113\0"+
    "\5\4\12\0\4\4\4\0\36\4\106\0\1\4\1\0"+
    "\10\4\12\0\246\4\66\0\2\4\11\0\27\4\6\0"+
    "\42\4\101\0\3\4\1\0\13\4\12\0\46\4\71\0"+
    "\7\4\12\0\66\4\33\0\2\4\17\0\4\4\12\0"+
    "\306\4\73\0\145\4\112\0\25\4\10\0\2\4\1\0"+
    "\2\4\10\0\1\4\2\0\1\4\36\0\1\4\2\0"+
    "\2\4\11\0\14\4\12\0\106\4\10\0\2\4\56\0"+
    "\2\4\10\0\1\4\2\0\33\4\77\0\10\4\1\0"+
    "\10\4\112\0\3\4\1\0\42\4\71\0\7\4\11\0"+
    "\1\4\55\0\1\4\11\0\17\4\12\0\30\4\36\0"+
    "\2\4\26\0\1\4\16\0\111\4\7\0\1\4\2\0"+
    "\1\4\54\0\3\4\1\0\1\4\2\0\1\4\11\0"+
    "\10\4\12\0\6\4\6\0\1\4\2\0\1\4\45\0"+
    "\1\4\2\0\1\4\6\0\7\4\12\0\u0136\4\27\0"+
    "\271\4\1\0\54\4\4\0\37\4\232\0\146\4\157\0"+
    "\21\4\304\0\274\4\57\0\1\4\11\0\307\4\107\0"+
    "\271\4\71\0\7\4\37\0\1\4\12\0\146\4\36\0"+
    "\2\4\5\0\13\4\67\0\11\4\4\0\14\4\12\0"+
    "\11\4\25\0\5\4\23\0\260\4\100\0\200\4\113\0"+
    "\4\4\71\0\7\4\21\0\100\4\2\0\1\4\2\0"+
    "\13\4\2\0\16\4\370\0\10\4\326\0\52\4\11\0"+
    "\367\4\37\0\61\4\3\0\21\4\4\0\10\4\u018c\0"+
    "\4\4\153\0\5\4\15\0\3\4\11\0\7\4\12\0"+
    "\3\4\2\0\1\4\4\0\301\4\5\0\3\4\26\0"+
    "\2\4\7\0\36\4\4\0\224\4\3\0\273\4\125\0"+
    "\1\4\107\0\1\4\2\0\2\4\1\0\2\4\2\0"+
    "\2\4\4\0\1\4\14\0\1\4\1\0\1\4\7\0"+
    "\1\4\101\0\1\4\4\0\2\4\10\0\1\4\7\0"+
    "\1\4\34\0\1\4\4\0\1\4\5\0\1\4\1\0"+
    "\3\4\7\0\1\4\u0154\0\2\4\31\0\1\4\31\0"+
    "\1\4\37\0\1\4\31\0\1\4\37\0\1\4\31\0"+
    "\1\4\37\0\1\4\31\0\1\4\37\0\1\4\31\0"+
    "\1\4\10\0\2\4\151\0\4\4\62\0\10\4\1\0"+
    "\16\4\1\0\26\4\5\0\1\4\17\0\120\4\7\0"+
    "\1\4\21\0\2\4\7\0\1\4\2\0\1\4\5\0"+
    "\325\4\55\0\3\4\16\0\2\4\12\0\4\4\1\0"+
    "\u0171\4\72\0\5\4\306\0\13\4\7\0\51\4\114\0"+
    "\4\4\12\0\u0156\4\1\0\117\4\4\0\1\4\33\0"+
    "\1\4\2\0\1\4\1\0\2\4\1\0\1\4\12\0"+
    "\1\4\4\0\1\4\1\0\1\4\1\0\6\4\1\0"+
    "\4\4\1\0\1\4\1\0\1\4\1\0\1\4\3\0"+
    "\1\4\2\0\1\4\1\0\2\4\1\0\1\4\1\0"+
    "\1\4\1\0\1\4\1\0\1\4\1\0\1\4\2\0"+
    "\1\4\1\0\2\4\4\0\1\4\7\0\1\4\4\0"+
    "\1\4\4\0\1\4\1\0\1\4\12\0\1\4\21\0"+
    "\5\4\3\0\1\4\5\0\1\4\21\0\u0134\4\12\0"+
    "\6\4\336\0\42\4\65\0\13\4\336\0\2\4\u0182\0"+
    "\16\4\u0131\0\37\4\36\0\342\4\113\0\266\4\1\0"+
    "\36\4\140\0\200\4\360\0\20\4";

  private static int [] zzUnpackcmap_blocks() {
    int [] result = new int[31232];
    int offset = 0;
    offset = zzUnpackcmap_blocks(ZZ_CMAP_BLOCKS_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackcmap_blocks(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }

  /**
   * Translates DFA states to action switch labels.
   */
  private static final int [] ZZ_ACTION = zzUnpackAction();

  private static final String ZZ_ACTION_PACKED_0 =
    "\1\0\1\1\1\2\2\3\1\4\1\5\1\6\1\7"+
    "\1\10\1\11\1\3\1\12\1\3\2\13\1\14\1\15"+
    "\1\16\1\17\1\20\4\1\1\21\1\22\1\2\1\23"+
    "\1\4\1\0\1\6\1\0\2\13\1\24\1\25\1\0"+
    "\2\13\4\1\2\13\1\0\3\13\3\1\1\26\1\24"+
    "\1\1\1\27\1\30\1\31";

  private static int [] zzUnpackAction() {
    int [] result = new int[59];
    int offset = 0;
    offset = zzUnpackAction(ZZ_ACTION_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackAction(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }


  /**
   * Translates a state to a row index in the transition table
   */
  private static final int [] ZZ_ROWMAP = zzUnpackRowMap();

  private static final String ZZ_ROWMAP_PACKED_0 =
    "\0\0\0\52\0\124\0\176\0\250\0\322\0\176\0\374"+
    "\0\176\0\176\0\176\0\u0126\0\176\0\u0150\0\u017a\0\u01a4"+
    "\0\176\0\176\0\176\0\176\0\176\0\u01ce\0\u01f8\0\u0222"+
    "\0\u024c\0\176\0\176\0\u0276\0\176\0\176\0\u02a0\0\176"+
    "\0\u02ca\0\u02f4\0\u031e\0\u0348\0\u0372\0\u039c\0\u03c6\0\52"+
    "\0\u03f0\0\u041a\0\u0444\0\u046e\0\u0498\0\176\0\u04c2\0\u04ec"+
    "\0\u0516\0\u0540\0\u056a\0\u0594\0\u05be\0\52\0\176\0\u05e8"+
    "\0\52\0\52\0\52";

  private static int [] zzUnpackRowMap() {
    int [] result = new int[59];
    int offset = 0;
    offset = zzUnpackRowMap(ZZ_ROWMAP_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackRowMap(String packed, int offset, int [] result) {
    int i = 0;  /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length() - 1;
    while (i < l) {
      int high = packed.charAt(i++) << 16;
      result[j++] = high | packed.charAt(i++);
    }
    return j;
  }

  /**
   * The transition table of the DFA
   */
  private static final int [] ZZ_TRANS = zzUnpacktrans();

  private static final String ZZ_TRANS_PACKED_0 =
    "\1\2\3\3\1\4\1\5\1\6\1\7\1\10\1\11"+
    "\1\12\2\4\1\13\1\14\1\15\1\16\1\17\1\20"+
    "\1\21\1\22\1\23\2\2\1\24\1\4\1\25\3\2"+
    "\1\26\1\2\1\27\2\2\1\30\1\2\1\31\1\32"+
    "\1\33\1\34\1\4\1\2\20\0\2\2\3\0\2\2"+
    "\3\0\13\2\2\0\1\2\2\0\3\3\44\0\1\3"+
    "\100\0\1\35\24\0\2\6\1\0\3\6\1\36\22\6"+
    "\1\37\20\6\2\10\1\0\5\10\1\40\20\10\1\41"+
    "\20\10\21\0\1\42\1\43\42\0\1\44\4\0\1\45"+
    "\31\0\1\2\16\0\1\46\1\0\2\2\3\0\1\47"+
    "\1\50\3\0\2\2\1\47\1\2\1\50\6\2\2\0"+
    "\1\2\1\0\1\2\16\0\1\46\1\0\2\20\3\0"+
    "\1\47\1\50\3\0\1\20\1\2\1\47\1\2\1\50"+
    "\6\2\2\0\1\2\1\0\1\2\20\0\2\2\3\0"+
    "\2\2\3\0\1\2\1\51\11\2\2\0\1\2\1\0"+
    "\1\2\20\0\2\2\3\0\2\2\3\0\11\2\1\52"+
    "\1\2\2\0\1\2\1\0\1\2\20\0\2\2\3\0"+
    "\2\2\3\0\6\2\1\53\4\2\2\0\1\2\1\0"+
    "\1\2\20\0\2\2\3\0\2\2\3\0\1\2\1\54"+
    "\11\2\2\0\1\2\1\0\1\2\3\3\15\0\2\2"+
    "\3\0\2\2\3\0\13\2\2\0\1\34\1\0\2\6"+
    "\1\0\47\6\2\10\1\0\47\10\17\0\1\46\6\0"+
    "\1\55\1\56\5\0\1\55\1\0\1\56\31\0\1\46"+
    "\1\0\2\43\3\0\1\55\1\56\3\0\1\43\1\0"+
    "\1\55\1\0\1\56\12\0\13\44\1\57\36\44\2\45"+
    "\2\0\44\45\23\0\2\60\27\0\1\2\13\0\1\61"+
    "\1\0\1\61\2\0\2\62\3\0\2\2\3\0\1\62"+
    "\12\2\2\0\1\2\1\0\1\2\20\0\2\2\3\0"+
    "\2\2\3\0\4\2\1\63\6\2\2\0\1\2\1\0"+
    "\1\2\20\0\2\2\3\0\2\2\3\0\4\2\1\64"+
    "\6\2\2\0\1\2\1\0\1\2\20\0\2\2\3\0"+
    "\2\2\3\0\11\2\1\65\1\2\2\0\1\2\1\0"+
    "\1\2\20\0\2\2\3\0\2\2\3\0\4\2\1\66"+
    "\6\2\2\0\1\2\15\0\1\61\1\0\1\61\2\0"+
    "\2\61\10\0\1\61\16\0\13\44\1\57\4\44\1\67"+
    "\31\44\21\0\2\60\3\0\1\55\6\0\1\55\35\0"+
    "\2\61\10\0\1\61\16\0\1\2\20\0\2\62\3\0"+
    "\2\2\3\0\1\62\12\2\2\0\1\2\1\0\1\2"+
    "\20\0\2\2\3\0\2\2\3\0\7\2\1\70\3\2"+
    "\2\0\1\2\1\0\1\2\20\0\2\2\3\0\2\2"+
    "\3\0\4\2\1\71\6\2\2\0\1\2\1\0\1\2"+
    "\20\0\2\2\3\0\2\2\3\0\2\2\1\72\10\2"+
    "\2\0\1\2\1\0\1\2\20\0\2\2\3\0\2\2"+
    "\3\0\2\2\1\73\10\2\2\0\1\2\1\0";

  private static int [] zzUnpacktrans() {
    int [] result = new int[1554];
    int offset = 0;
    offset = zzUnpacktrans(ZZ_TRANS_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpacktrans(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      value--;
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }


  /* error codes */
  private static final int ZZ_UNKNOWN_ERROR = 0;
  private static final int ZZ_NO_MATCH = 1;
  private static final int ZZ_PUSHBACK_2BIG = 2;

  /* error messages for the codes above */
  private static final String[] ZZ_ERROR_MSG = {
    "Unknown internal scanner error",
    "Error: could not match input",
    "Error: pushback value was too large"
  };

  /**
   * ZZ_ATTRIBUTE[aState] contains the attributes of state {@code aState}
   */
  private static final int [] ZZ_ATTRIBUTE = zzUnpackAttribute();

  private static final String ZZ_ATTRIBUTE_PACKED_0 =
    "\1\0\2\1\1\11\2\1\1\11\1\1\3\11\1\1"+
    "\1\11\3\1\5\11\4\1\2\11\1\1\2\11\1\0"+
    "\1\11\1\0\4\1\1\0\7\1\1\11\1\0\7\1"+
    "\1\11\4\1";

  private static int [] zzUnpackAttribute() {
    int [] result = new int[59];
    int offset = 0;
    offset = zzUnpackAttribute(ZZ_ATTRIBUTE_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackAttribute(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }

  /** the input device */
  private java.io.Reader zzReader;

  /** the current state of the DFA */
  private int zzState;

  /** the current lexical state */
  private int zzLexicalState = YYINITIAL;

  /** this buffer contains the current text to be matched and is
      the source of the yytext() string */
  private CharSequence zzBuffer = "";

  /** the textposition at the last accepting state */
  private int zzMarkedPos;

  /** the current text position in the buffer */
  private int zzCurrentPos;

  /** startRead marks the beginning of the yytext() string in the buffer */
  private int zzStartRead;

  /** endRead marks the last character in the buffer, that has been read
      from input */
  private int zzEndRead;

  /** zzAtEOF == true <=> the scanner is at the EOF */
  private boolean zzAtEOF;

  /** Number of newlines encountered up to the start of the matched text. */
  @SuppressWarnings("unused")
  private int yyline;

  /** Number of characters from the last newline up to the start of the matched text. */
  @SuppressWarnings("unused")
  protected int yycolumn;

  /** Number of characters up to the start of the matched text. */
  @SuppressWarnings("unused")
  private long yychar;

  /** Whether the scanner is currently at the beginning of a line. */
  @SuppressWarnings("unused")
  private boolean zzAtBOL = true;

  /** Whether the user-EOF-code has already been executed. */
  @SuppressWarnings("unused")
  private boolean zzEOFDone;

  /* user code: */
  public _AmperLexer() {
    this((java.io.Reader)null);
  }


  /**
   * Creates a new scanner
   *
   * @param   in  the java.io.Reader to read input from.
   */
  public _AmperLexer(java.io.Reader in) {
    this.zzReader = in;
  }


  /** Returns the maximum size of the scanner buffer, which limits the size of tokens. */
  private int zzMaxBufferLen() {
    return Integer.MAX_VALUE;
  }

  /**  Whether the scanner buffer can grow to accommodate a larger token. */
  private boolean zzCanGrow() {
    return true;
  }

  /**
   * Translates raw input code points to DFA table row
   */
  private static int zzCMap(int input) {
    int offset = input & 255;
    return offset == input ? ZZ_CMAP_BLOCKS[offset] : ZZ_CMAP_BLOCKS[ZZ_CMAP_TOP[input >> 8] | offset];
  }

  public final int getTokenStart() {
    return zzStartRead;
  }

  public final int getTokenEnd() {
    return getTokenStart() + yylength();
  }

  public void reset(CharSequence buffer, int start, int end, int initialState) {
    zzBuffer = buffer;
    zzCurrentPos = zzMarkedPos = zzStartRead = start;
    zzAtEOF  = false;
    zzAtBOL = true;
    zzEndRead = end;
    yybegin(initialState);
  }

  /**
   * Refills the input buffer.
   *
   * @return      {@code false}, iff there was new input.
   *
   * @exception   java.io.IOException  if any I/O-Error occurs
   */
  private boolean zzRefill() throws java.io.IOException {
    return true;
  }


  /**
   * Returns the current lexical state.
   */
  public final int yystate() {
    return zzLexicalState;
  }


  /**
   * Enters a new lexical state
   *
   * @param newState the new lexical state
   */
  public final void yybegin(int newState) {
    zzLexicalState = newState;
  }


  /**
   * Returns the text matched by the current regular expression.
   */
  public final CharSequence yytext() {
    return zzBuffer.subSequence(zzStartRead, zzMarkedPos);
  }


  /**
   * Returns the character at position {@code pos} from the
   * matched text.
   *
   * It is equivalent to yytext().charAt(pos), but faster
   *
   * @param pos the position of the character to fetch.
   *            A value from 0 to yylength()-1.
   *
   * @return the character at position pos
   */
  public final char yycharat(int pos) {
    return zzBuffer.charAt(zzStartRead+pos);
  }


  /**
   * Returns the length of the matched text region.
   */
  public final int yylength() {
    return zzMarkedPos-zzStartRead;
  }


  /**
   * Reports an error that occurred while scanning.
   *
   * In a wellformed scanner (no or only correct usage of
   * yypushback(int) and a match-all fallback rule) this method
   * will only be called with things that "Can't Possibly Happen".
   * If this method is called, something is seriously wrong
   * (e.g. a JFlex bug producing a faulty scanner etc.).
   *
   * Usual syntax/scanner level error handling should be done
   * in error fallback rules.
   *
   * @param   errorCode  the code of the errormessage to display
   */
  private void zzScanError(int errorCode) {
    String message;
    try {
      message = ZZ_ERROR_MSG[errorCode];
    }
    catch (ArrayIndexOutOfBoundsException e) {
      message = ZZ_ERROR_MSG[ZZ_UNKNOWN_ERROR];
    }

    throw new Error(message);
  }


  /**
   * Pushes the specified amount of characters back into the input stream.
   *
   * They will be read again by then next call of the scanning method
   *
   * @param number  the number of characters to be read again.
   *                This number must not be greater than yylength()!
   */
  public void yypushback(int number)  {
    if ( number > yylength() )
      zzScanError(ZZ_PUSHBACK_2BIG);

    zzMarkedPos -= number;
  }


  /**
   * Resumes scanning until the next regular expression is matched,
   * the end of input is encountered or an I/O-Error occurs.
   *
   * @return      the next token
   * @exception   java.io.IOException  if any I/O-Error occurs
   */
  public IElementType advance() throws java.io.IOException
  {
    int zzInput;
    int zzAction;

    // cached fields:
    int zzCurrentPosL;
    int zzMarkedPosL;
    int zzEndReadL = zzEndRead;
    CharSequence zzBufferL = zzBuffer;

    int [] zzTransL = ZZ_TRANS;
    int [] zzRowMapL = ZZ_ROWMAP;
    int [] zzAttrL = ZZ_ATTRIBUTE;

    while (true) {
      zzMarkedPosL = zzMarkedPos;

      zzAction = -1;

      zzCurrentPosL = zzCurrentPos = zzStartRead = zzMarkedPosL;

      zzState = ZZ_LEXSTATE[zzLexicalState];

      // set up zzAction for empty match case:
      int zzAttributes = zzAttrL[zzState];
      if ( (zzAttributes & 1) == 1 ) {
        zzAction = zzState;
      }


      zzForAction: {
        while (true) {

          if (zzCurrentPosL < zzEndReadL) {
            zzInput = Character.codePointAt(zzBufferL, zzCurrentPosL);
            zzCurrentPosL += Character.charCount(zzInput);
          }
          else if (zzAtEOF) {
            zzInput = YYEOF;
            break zzForAction;
          }
          else {
            // store back cached positions
            zzCurrentPos  = zzCurrentPosL;
            zzMarkedPos   = zzMarkedPosL;
            boolean eof = zzRefill();
            // get translated positions and possibly new buffer
            zzCurrentPosL  = zzCurrentPos;
            zzMarkedPosL   = zzMarkedPos;
            zzBufferL      = zzBuffer;
            zzEndReadL     = zzEndRead;
            if (eof) {
              zzInput = YYEOF;
              break zzForAction;
            }
            else {
              zzInput = Character.codePointAt(zzBufferL, zzCurrentPosL);
              zzCurrentPosL += Character.charCount(zzInput);
            }
          }
          int zzNext = zzTransL[ zzRowMapL[zzState] + zzCMap(zzInput) ];
          if (zzNext == -1) break zzForAction;
          zzState = zzNext;

          zzAttributes = zzAttrL[zzState];
          if ( (zzAttributes & 1) == 1 ) {
            zzAction = zzState;
            zzMarkedPosL = zzCurrentPosL;
            if ( (zzAttributes & 8) == 8 ) break zzForAction;
          }

        }
      }

      // store back cached position
      zzMarkedPos = zzMarkedPosL;

      if (zzInput == YYEOF && zzStartRead == zzCurrentPos) {
        zzAtEOF = true;
        return null;
      }
      else {
        switch (zzAction < 0 ? zzAction : ZZ_ACTION[zzAction]) {
          case 1:
            { return IDENTIFIER;
            }
          // fall through
          case 26: break;
          case 2:
            { return WHITE_SPACE;
            }
          // fall through
          case 27: break;
          case 3:
            { return BAD_CHARACTER;
            }
          // fall through
          case 28: break;
          case 4:
            { return DOUBLE_QUOTED_STRING;
            }
          // fall through
          case 29: break;
          case 5:
            { return SHARP;
            }
          // fall through
          case 30: break;
          case 6:
            { return SINGLE_QUOTED_STRING;
            }
          // fall through
          case 31: break;
          case 7:
            { return L_PAREN;
            }
          // fall through
          case 32: break;
          case 8:
            { return R_PAREN;
            }
          // fall through
          case 33: break;
          case 9:
            { return COMMA;
            }
          // fall through
          case 34: break;
          case 10:
            { return DOT;
            }
          // fall through
          case 35: break;
          case 11:
            { return NUMBER;
            }
          // fall through
          case 36: break;
          case 12:
            { return COLON;
            }
          // fall through
          case 37: break;
          case 13:
            { return EQ;
            }
          // fall through
          case 38: break;
          case 14:
            { return AT;
            }
          // fall through
          case 39: break;
          case 15:
            { return L_BRACKET;
            }
          // fall through
          case 40: break;
          case 16:
            { return R_BRACKET;
            }
          // fall through
          case 41: break;
          case 17:
            { return L_CURLY;
            }
          // fall through
          case 42: break;
          case 18:
            { return R_CURLY;
            }
          // fall through
          case 43: break;
          case 19:
            { return NEGAT;
            }
          // fall through
          case 44: break;
          case 20:
            { return BLOCK_COMMENT;
            }
          // fall through
          case 45: break;
          case 21:
            { return LINE_COMMENT;
            }
          // fall through
          case 46: break;
          case 22:
            { return VAL_KEYWORD;
            }
          // fall through
          case 47: break;
          case 23:
            { return NULL;
            }
          // fall through
          case 48: break;
          case 24:
            { return TRUE;
            }
          // fall through
          case 49: break;
          case 25:
            { return FALSE;
            }
          // fall through
          case 50: break;
          default:
            zzScanError(ZZ_NO_MATCH);
          }
      }
    }
  }


}
