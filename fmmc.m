function [ B, cvx_optval ] = fmmc(A)

% [W,S] = FMMC(A) gives a vector of the fastest mixing Markov chain
% edge weights for a graph described by the incidence matrix A (n x m).
% Here n is the number of nodes and m is the number of edges in the graph;
% each column of A has exactly one +1 and one -1.
%
% The FMMC edge weights are given the SDP:
%
%   minimize    s
%   subject to  -s*I <= I - L - (1/n)11' <= s*I
%               w >= 0,  diag(L) <= 1
%
% where the variables are edge weights w in R^m and s in R.
% Here L is the weighted Laplacian defined by L = A*diag(w)*A'.
% The optimal value is s, and is returned in the second output.
%
% For more details see references:
% "Fastest mixing Markov chain on a graph" by S. Boyd, P. Diaconis, and L. Xiao
% "Convex Optimization of Graph Laplacian Eigenvalues" by S. Boyd
%
% Written for CVX by Almir Mutapcic 08/29/06

% A = [0 1 0 0;1 0 1 1;0 1 0 1;0 1 1 0];
% disp('kokok')
A = javaCellArgs2Matlab(A);

A = adj2inc(A);

A = A';

[n,m] = size(A);
I = eye(n,n);
J = I - (1/n)*ones(n,n);
cvx_begin sdp
    variable w(m,1)   % edge weights
    variable s        % epigraph variable
    variable L(n,n) symmetric
    minimize( s )
    subject to
        L == A * diag(w) * A';
        J - L <= +s * I;
        J - L >= -s * I;
        w >= 0;
        diag(L) <= 1;
cvx_end

B = zeros(size(A,1),size(A,1));
for i=1:size(A,2)
    ind = find(A(:,i));
    B(ind(1),ind(2)) = w(i);
    B(ind(2),ind(1)) = w(i);
    
end
