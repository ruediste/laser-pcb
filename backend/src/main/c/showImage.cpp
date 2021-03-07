#include "showImage.h"

Point toImg(const Point p)
{
  double const scale = 600. / 24;
  return Point((p.x() + 12) * scale, (p.y() + 12) * scale);
}

const unsigned char red[] = {255, 0, 0}, green[] = {0, 255, 0}, blue[] = {0, 0, 255};

void ShowImage::draw(Ss const &ss)
{
  typedef typename Ss::Vertex_const_handle Vertex_const_handle;
  typedef typename Ss::Halfedge_const_handle Halfedge_const_handle;
  typedef typename Ss::Halfedge_const_iterator Halfedge_const_iterator;

  Halfedge_const_handle null_halfedge;
  Vertex_const_handle null_vertex;

  for (Halfedge_const_iterator i = ss.halfedges_begin(); i != ss.halfedges_end(); ++i)
  {
    Point src = toImg(i->opposite()->vertex()->point());
    Point dst = toImg(i->vertex()->point());
    if (!i->is_bisector())
      visu.draw_line((int)src.x(), (int)src.y(), (int)dst.x(), (int)dst.y(), i->is_bisector() ? green : red);
  }
}

struct Vector
{
  double x;
  double y;
  Vector(Point &p1, Point &p2)
  {
    x = p2.x() - p1.x();
    y = p2.y() - p1.y();
  }
  Vector(double x, double y) : x(x), y(y) {}
  double length()
  {
    return sqrt(x * x + y * y);
  }

  Vector operator*(double factor)
  {
    return Vector(x * factor, y * factor);
  }
  Vector normalize()
  {
    return (*this) * (1. / length());
  }

  Vector operator+(const Vector &other)
  {
    return Vector(x + other.x, y + other.y);
  }

  Vector operator-()
  {
    return Vector(-x, -y);
  }

  double dot(const Vector &other)
  {
    return x + other.x + y * other.y;
  }

  void print()
  {

    std::cout << "(" << x << "," << y << ")";
  }
};

void print_point(const Point &p)
{
  std::cout << "(" << p.x() << "," << p.y() << ")";
}

void ShowImage::drawLine(const Point &src, const Point &dst, const unsigned char *color)
{
  Point p1 = toImg(src);
  Point p2 = toImg(dst);
  visu.draw_line((int)p1.x(), (int)p1.y(), (int)p2.x(), (int)p2.y(), blue);
}

void handleUncut(ShowImage &showImage, Point &p1, Point &p2, Point &p3)
{
  Vector v1(p2, p1);
  Vector v2(p2, p3);
  v1 = v1.normalize();
  v2 = v2.normalize();
  v1.print();
  v2.print();
  Vector d = -(v1 + v2).normalize();
  d.print();
   double c = v1.dot(v2);
  // d = d * c;
  d=d*3*c;

  Point p = Point(p2.x() + d.x, p2.y() + d.y);
  print_point(p2);
  std::cout << "->";
  print_point(p);
  std::cout << "\n";
  showImage.drawLine(p2, p, blue);
}

void ShowImage::draw(PolygonPtrVector const &polies)
{
  typedef std::vector<boost::shared_ptr<Polygon_2>> PolygonVector;

  //std::cout << "Polygon list with " << polies.size() << " polygons" << std::endl;

  for (typename PolygonVector::const_iterator pi = polies.begin(); pi != polies.end(); ++pi)
  {
    bool first = true;
    Point firstPoint;
    Point prev[2];
    int i = 0;
    for (typename Polygon_2::Vertex_const_iterator vi = (**pi).vertices_begin(); vi != (**pi).vertices_end(); ++vi)
    {
      if (i == 0)
        firstPoint = *vi;
      else
        drawLine(prev[1], *vi, blue);

      if (i > 1)
      {
        handleUncut(*this, prev[0], prev[1], *vi);
      }
      prev[0] = prev[1];
      prev[1] = *vi;
      i++;
    }
    if (i > 0)
      drawLine(prev[1], firstPoint, blue);
    if (i > 1)
      handleUncut(*this, prev[0], prev[1], firstPoint);
  }
}

void ShowImage::display()
{
  cimg_library::CImgDisplay visu_disp(visu, "Preview");
  while (!visu_disp.is_closed())
  {
    visu_disp.wait();
    if (visu_disp.button())
      break;
  }
}