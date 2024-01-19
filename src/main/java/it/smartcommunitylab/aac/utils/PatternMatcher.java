/*******************************************************************************

 * Copyright 2015-2019 Smart Community Lab, FBK
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/

package it.smartcommunitylab.aac.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Validator for the URI patterns. Provides a method to verify whether
 * two patterns of the form abc{placeholder}xyz may accept the same strings.
 * @author raman
 *
 */
public class PatternMatcher {

    private static final int AST = -1;

    private int[] s1, s2;
    private List<int[]> pool = new ArrayList<int[]>();

    public PatternMatcher(String s1, String s2) {
        this.s1 = convert(s1);
        this.s2 = convert(s2);
    }

    /**
     * Compute for the two URI patterns the acceptance of the same instances.
     * @return true if the patterns may accept the same values
     */
    public boolean compute() {
        int pos1 = 0;
        int pos2 = 0;
        int[] point = new int[] { pos1, pos2 };
        pool.add(point);

        while (!pool.isEmpty()) {
            point = pool.get(0);
            pos1 = point[0];
            pos2 = point[1];
            if (pos1 == s1.length && pos2 == s2.length) {
                return true;
            }
            if (pos1 != s1.length && pos2 != s2.length) {
                int l1 = s1[pos1], l2 = s2[pos2];
                if (match(l1, l2, pos1, pos2)) {
                    //
                }
            }
            pool.remove(0);
        }
        return false;
    }

    private boolean match(int l1, int l2, int pos1, int pos2) {
        boolean b = false;
        if (l1 == l2 && l1 == AST) {
            b = pool.add(new int[] { pos1, pos2 + 1 });
            if (!b) {
                System.err.println("!");
            }
            b = pool.add(new int[] { pos1 + 1, pos2 });
            if (!b) {
                System.err.println("!");
            }
            b = pool.add(new int[] { pos1 + 1, pos2 + 1 });
            if (!b) {
                System.err.println("!");
            }
        } else if (l1 == l2) {
            b = pool.add(new int[] { pos1 + 1, pos2 + 1 });
            if (!b) {
                System.err.println("!");
            }
        } else if (l1 == AST) {
            b = pool.add(new int[] { pos1, pos2 + 1 });
            if (!b) {
                System.err.println("!");
            }
            b = pool.add(new int[] { pos1 + 1, pos2 + 1 });
            if (!b) {
                System.err.println("!");
            }
        } else if (l2 == AST) {
            b = pool.add(new int[] { pos1 + 1, pos2 });
            if (!b) {
                System.err.println("!");
            }
            b = pool.add(new int[] { pos1 + 1, pos2 + 1 });
            if (!b) {
                System.err.println("!");
            }
        } else {
            return false;
        }
        return true;
    }

    private static int[] convert(String s) {
        List<Integer> list = new ArrayList<Integer>();
        boolean wc = false;
        char c = 0;
        for (int i = 0; i < s.length(); i++) {
            c = s.charAt(i);
            if (c == '{') {
                wc = true;
                list.add(AST);
                continue;
            } else if (c == '}') {
                wc = false;
                continue;
            } else if (wc) {
                continue;
            } else {
                list.add((int) c);
            }
        }
        if (wc) throw new IllegalArgumentException("Not a well-formed pattern: " + s);
        int[] a = new int[list.size()];
        for (int i = 0; i < list.size(); i++) a[i] = list.get(i);
        return a;
    }
}
