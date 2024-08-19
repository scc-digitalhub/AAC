import { TableHead, TableRow, TableCell } from '@mui/material';
import React from 'react';
import { DatagridHeaderProps, FieldProps } from 'react-admin';

export const DataGridBlankHeader = ({ children }: DatagridHeaderProps) => (
    <TableHead>
        <TableRow>
            {React.Children.map(children, child =>
                React.isValidElement<FieldProps>(child) ? (
                    <TableCell key={child.props.source}></TableCell>
                ) : null
            )}
        </TableRow>
    </TableHead>
);
