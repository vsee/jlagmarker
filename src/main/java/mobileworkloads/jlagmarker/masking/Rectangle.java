/**
*
* Copyright 2017 Volker Seeker <volker@seekerscience.co.uk>.
*
* This file is part of JLagmarker.
*
* JLagmarker is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* JLagmarker is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with JLagmarker. If not, see <http://www.gnu.org/licenses/>.
*
*/
package mobileworkloads.jlagmarker.masking;

public class Rectangle {
	public final int x0;
	public final int y0;
	public final int width;
	public final int height;
	
	public Rectangle(int x0, int y0, int width, int height) {
		this.x0 = x0;
		this.y0 = y0;
		this.width = width;
		this.height = height;
		
		assert x0 >= 0 && y0 >= 0 && width > 0 && height > 0 : "Invalid Rectangle dimensions: " + toString();
	}
	
	@Override
	public String toString() {
		StringBuilder bld = new StringBuilder();
		bld.append(x0).append(" ")
		.append(y0).append(" ")
		.append(width).append(" ")
		.append(height);
		return bld.toString();
	}
}
