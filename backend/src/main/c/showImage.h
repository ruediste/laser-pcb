#ifndef SHOW_IMAGE_H
#define SHOW_IMAGE_H

#include <CGAL/Exact_predicates_inexact_constructions_kernel.h>
#include <CGAL/create_straight_skeleton_2.h>
#include "CImg.h"

typedef CGAL::Exact_predicates_inexact_constructions_kernel K;
typedef CGAL::Straight_skeleton_2<K> Ss;
typedef CGAL::Polygon_2<K> Polygon_2;
typedef boost::shared_ptr<Polygon_2> PolygonPtr;
typedef std::vector<PolygonPtr> PolygonPtrVector;
typedef K::Point_2 Point;

class ShowImage
{
public:
    cimg_library::CImg<unsigned char> visu;
    ShowImage() : visu(600, 600, 1, 3, 0)
    {
    }
    void draw(Ss const &ss);

    void draw(PolygonPtrVector const &polies);
    void display();

    void drawLine(const Point &src, const Point &dst, const unsigned char *color);
};

#endif