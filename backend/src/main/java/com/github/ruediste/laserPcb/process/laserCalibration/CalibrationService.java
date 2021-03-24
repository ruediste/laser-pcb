package com.github.ruediste.laserPcb.process.laserCalibration;

import java.util.List;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.SimpleBounds;
import org.apache.commons.math3.optim.SimplePointChecker;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.CMAESOptimizer;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.PowellOptimizer;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.springframework.stereotype.Service;

@Service
public class CalibrationService {

	public static class LaserParameters {
		public double P;
		public double d;

		@Override
		public String toString() {
			return "LaserParameters [P=" + P + ", d=" + d + "]";
		}

		public double area() {
			return Math.PI * d * d / 4;
		}

		public double minExposureTime() {
			return area() / P;
		}

	}

	public static class ToolConfig {
		public LaserParameters params;
		public double v;

		public double lineWidth() {
			return Math.sqrt(lineWidth2());
		}

		public double lineWidth2() {
			double l = v * params.minExposureTime();
			return params.d * params.d - l * l;
		}
	}

	public static class CalibrationMeasurement {
		public double v;
		public double w;

		public CalibrationMeasurement() {
		}

		public CalibrationMeasurement(double v, double w) {
			this.v = v;
			this.w = w;
		}

	}

	public LaserParameters calculateParameters(List<CalibrationMeasurement> measurements) {
		var optimizer = new PowellOptimizer(1e-6, 1e-6);
		PointValuePair optimum = optimizer.optimize(new ObjectiveFunction(new MultivariateFunction() {

			@Override
			public double value(double[] point) {
				double d = point[0];
				double P = point[1];
				double errorSum = 0;
				ToolConfig cfg = new ToolConfig();
				cfg.params = new LaserParameters();
				cfg.params.P = P;
				cfg.params.d = d;
				for (var measurement : measurements) {

//					double error = measurement.v * measurement.v * Math.PI * Math.PI * Math.pow(d, 4) / (16 * P * P)
//							+ measurement.w * measurement.w - d * d;
					cfg.v = measurement.v;
					double error = cfg.lineWidth2() - measurement.w * measurement.w;
					errorSum += Math.abs(error);

				}
				return errorSum;
			}
		}), GoalType.MINIMIZE, new InitialGuess(new double[] { 0.25, 1 }), new MaxEval(1000));
		LaserParameters params = new LaserParameters();
		params.d = Math.abs(optimum.getFirst()[0]);
		params.P = Math.abs(optimum.getFirst()[1]);
		return params;
	}

	public LaserParameters calculateParameters1(List<CalibrationMeasurement> measurements) {

		CMAESOptimizer optimizer = new CMAESOptimizer(1000, 0, true, 2, 5, new JDKRandomGenerator(), true,
				new SimplePointChecker<>(0.0001, -1));
		PointValuePair optimum = optimizer.optimize(new ObjectiveFunction(new MultivariateFunction() {

			@Override
			public double value(double[] point) {
				double d = point[0];
				double P = point[1];
				double errorSum = 0;
				ToolConfig cfg = new ToolConfig();
				cfg.params = new LaserParameters();
				cfg.params.P = P;
				cfg.params.d = d;
				for (var measurement : measurements) {

//					double error = measurement.v * measurement.v * Math.PI * Math.PI * Math.pow(d, 4) / (16 * P * P)
//							+ measurement.w * measurement.w - d * d;
					cfg.v = measurement.v;
					double lineWidth2 = cfg.lineWidth2();
					double error;
					if (lineWidth2 < 0)
						error = measurement.w - lineWidth2;
					else
						error = Math.sqrt(lineWidth2) - measurement.w;
					errorSum += error * error;

				}
				if (Double.isNaN(errorSum))
					return Double.POSITIVE_INFINITY;
				return errorSum;
			}
		}), GoalType.MINIMIZE, new CMAESOptimizer.PopulationSize(8), new InitialGuess(new double[] { 0.25, 1 }),
				new CMAESOptimizer.Sigma(new double[] { 1, 1 }),
				new SimpleBounds(new double[] { 0, 0 }, new double[] { 1, 10 }), new MaxEval(1000));
		System.out.println("Error: " + optimum.getSecond());
		optimizer.getStatisticsFitnessHistory().forEach(System.out::println);
		LaserParameters params = new LaserParameters();
		params.d = optimum.getFirst()[0];
		params.P = optimum.getFirst()[1];
		return params;
	}
}
