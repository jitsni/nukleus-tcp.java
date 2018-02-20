/**
 * Copyright 2016-2017 The Reaktivity Project
 *
 * The Reaktivity Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.reaktivity.nukleus.tcp.internal.stream;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class RegionsManager
{

    private static final Comparator<Region> REGION_COMPARATOR = Comparator.comparingLong(o -> o.address);
    private List<Region> regions = new ArrayList<>();

    void add(long address, int length)
    {
        //System.out.printf("before add (%d, %d) regions=%s\n", address, length, regions);
        regions.add(new Region(address, length));
        regions.sort(REGION_COMPARATOR);

        // coalesce
        regions = coalesce(regions);
        //System.out.printf("after add (%d, %d) regions=%s\n", address, length, regions);
    }

    private static List<Region> coalesce(List<Region> regions)
    {
        // coalesce
        List<Region> newRegions = new ArrayList<>();
        for(Region region : regions)
        {
            if (newRegions.isEmpty())
            {
                newRegions.add(new Region(region.address, region.length));
            }
            else
            {
                Region last = newRegions.get(newRegions.size() - 1);
                if (region.address < last.address + last.length)
                {
                    throw new RuntimeException("Overlapping regions");
                }
                else if (last.address + last.length == region.address)
                {
                    last.update(last.length + region.length);
                }
                else
                {
                    newRegions.add(new Region(region.address, region.length));
                }
            }
        }
        return newRegions;
    }

    void remove(long address, int length)
    {

        //System.out.printf("before remove (%d, %d) regions=%s\n", address, length, regions);

        List<Region> newRegions = new ArrayList<>();
        for(Region region : regions)
        {
            if (address >= region.address && address < region.address + region.length)
            {
                if (address + length > region.address + region.length)
                {
                    throw new RuntimeException("Removing a bigger sub region");
                }
                else
                {
                    int part1Length = (int) (address - region.address);
                    int part2Length = region.length - part1Length - length;
                    if (part1Length > 0)
                    {
                        newRegions.add(new Region(region.address, part1Length));
                    }
                    if (part2Length > 0)
                    {
                        long part2Address = address + length;
                        newRegions.add(new Region(part2Address, part2Length));
                    }
                }
            }
            else
            {
                newRegions.add(new Region(region.address, region.length));
            }
        }

        regions = newRegions;
        //System.out.printf("after remove (%d, %d) regions=%s\n", address, length, regions);

    }

    void print()
    {
        System.out.println("TCP " + regions);
    }

    static final class Region
    {
        final long address;
        int length;

        Region(long address, int length)
        {
            this.address = address;
            this.length = length;
        }

        void update(int length)
        {
            this.length = length;
        }

        @Override
        public String toString()
        {
            return String.format("(%d, %d)", address, length);
        }

    }

}
