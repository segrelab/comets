package edu.bu.segrelab.comets.test.integration;

/* Copyright 2017, Gurobi Optimization, Inc. */

/* Want to cover three different sets but subject to a common budget of
   elements allowed to be used. However, the sets have different priorities to
   be covered; and we tackle this by using multi-objective optimization. */

import gurobi.*;

public class Multiobj {
  public static void main(String[] args) {

    try {
      // Sample data
      int groundSetSize = 20;
      int nSubsets      = 4;
      int Budget        = 12;
      double Set[][] = new double[][]
      { { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
        { 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1 },
        { 0, 0, 0, 1, 1, 0, 1, 1, 0, 0, 0, 0, 0, 1, 1, 0, 1, 1, 0, 0 },
        { 0, 0, 0, 1, 1, 1, 0, 0, 0, 1, 1, 1, 0, 0, 0, 1, 1, 1, 0, 0 } };
      int    SetObjPriority[] = new int[] {3, 2, 2, 1};
      double SetObjWeight[]   = new double[] {1.0, 0.25, 1.25, 1.0};
      int e, i, status, nSolutions;

      // Create environment
      GRBEnv env = new GRBEnv("Multiobj.log");

      // Create initial model
      GRBModel model = new GRBModel(env);
      model.set(GRB.StringAttr.ModelName, "Multiobj");

      // Initialize decision variables for ground set:
      // x[e] == 1 if element e is chosen for the covering.
      GRBVar[] Elem = model.addVars(groundSetSize, GRB.BINARY);
      for (e = 0; e < groundSetSize; e++) {
        String vname = "El" + String.valueOf(e);
        Elem[e].set(GRB.StringAttr.VarName, vname);
      }

      // Constraint: limit total number of elements to be picked to be at most
      // Budget
      GRBLinExpr lhs = new GRBLinExpr();
      for (e = 0; e < groundSetSize; e++) {
        lhs.addTerm(1.0, Elem[e]);
      }
      model.addConstr(lhs, GRB.LESS_EQUAL, Budget, "Budget");

      // Set global sense for ALL objectives
      model.set(GRB.IntAttr.ModelSense, GRB.MAXIMIZE);

      // Limit how many solutions to collect
      model.set(GRB.IntParam.PoolSolutions, 100);

      // Set number of objectives
      model.set(GRB.IntAttr.NumObj, 4);

      // Set and configure i-th objective
      for (i = 0; i < nSubsets; i++) {
        model.set(GRB.IntParam.ObjNumber, i);
        model.set(GRB.IntAttr.ObjNPriority, SetObjPriority[i]);
        model.set(GRB.DoubleAttr.ObjNWeight, SetObjWeight[i]);

        String vname = "Set" + String.valueOf(i);
        model.set(GRB.StringAttr.ObjNName, vname);
        model.set(GRB.DoubleAttr.ObjNRelTol, 0.01);
        model.set(GRB.DoubleAttr.ObjNAbsTol, 1.0 + i);
        model.set(GRB.DoubleAttr.ObjN, Elem, Set[i], 0, groundSetSize);
      }

      // Save problem
      model.write("Multiobj.lp");

      // Optimize
      model.optimize();

      // Status checking
      status = model.get(GRB.IntAttr.Status);

      if (status == GRB.INF_OR_UNBD ||
          status == GRB.INFEASIBLE  ||
          status == GRB.UNBOUNDED     ) {
        System.out.println("The model cannot be solved " +
                 "because it is infeasible or unbounded");
        System.exit(1);
      }
      if (status != GRB.OPTIMAL) {
        System.out.println("Optimization was stopped with status " + status);
        System.exit(1);
      }

      // Print best selected set
      System.out.println("Selected elements in best solution:");
      System.out.println("\t");
      for (e = 0; e < groundSetSize; e++) {
        if (Elem[e].get(GRB.DoubleAttr.X) < .9) continue;
        System.out.print(" El" + e);
      }
      System.out.println();

      // Print number of solutions stored
      nSolutions = model.get(GRB.IntAttr.SolCount);
      System.out.println("Number of solutions found: " + nSolutions);

      // Print objective values of solutions
      if (nSolutions > 10) nSolutions = 10;
      System.out.println("Objective values for first " + nSolutions);
      System.out.println(" solutions:");
      for (i = 0; i < nSubsets; i++) {
        model.set(GRB.IntParam.ObjNumber, i);

        System.out.print("\tSet" + i);
        for (e = 0; e < nSolutions; e++) {
          System.out.print(" ");
          model.set(GRB.IntParam.SolutionNumber, e);
          double val = model.get(GRB.DoubleAttr.ObjNVal);
          System.out.print("      " + val);
        }
        System.out.println();
      }
      model.dispose();
      env.dispose();
    } catch (GRBException e) {
      System.out.println("Error code = " + e.getErrorCode());
      System.out.println(e.getMessage());
    }
  }
}
