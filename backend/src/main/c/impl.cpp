#include "com_github_ruediste_laserPcb_jni_HelloWorldJNI.h"
#include <iostream>
#include <CGAL/Exact_predicates_inexact_constructions_kernel.h>
#include <CGAL/Polygon_2.h>
#include <CGAL/create_straight_skeleton_2.h>
#include <CGAL/create_offset_polygons_2.h>
#include "print.h"
#include <boost/shared_ptr.hpp>
#include <cassert>
#include "showImage.h"

typedef CGAL::Exact_predicates_inexact_constructions_kernel K;
typedef K::Point_2 Point;
typedef CGAL::Polygon_2<K> Polygon_2;
typedef CGAL::Straight_skeleton_2<K> Ss;
typedef boost::shared_ptr<Ss> SsPtr;

int main()
{
  Java_com_github_ruediste_laserPcb_jni_HelloWorldJNI_sayHello(NULL, NULL);
}

ShowImage showImage;

JNIEXPORT void JNICALL Java_com_github_ruediste_laserPcb_jni_HelloWorldJNI_sayHello(JNIEnv *, jobject)
{
  std::cout << "Hello from C++ !!" << std::endl;
  Polygon_2 poly;
  poly.push_back(Point(-1, -1));
  poly.push_back(Point(0, -12));
  poly.push_back(Point(1, -1));
  poly.push_back(Point(12, 0));
  poly.push_back(Point(1, 6));
  poly.push_back(Point(0, 12));
  poly.push_back(Point(-1, 1));
  poly.push_back(Point(-12, 0));
  assert(poly.is_counterclockwise_oriented());
  // You can pass the polygon via an iterator pair
  SsPtr iss = CGAL::create_interior_straight_skeleton_2(poly.vertices_begin(), poly.vertices_end());
  print_straight_skeleton(*iss);


  PolygonPtrVector offset_polygons = CGAL::create_offset_polygons_2<Polygon_2>(0.5, *iss);
  print_polygons(offset_polygons);

  showImage.draw(*iss);
  showImage.draw(offset_polygons);
  showImage.display();
}
