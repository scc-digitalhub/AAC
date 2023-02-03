import { Datagrid, DateField, List, NumberField, TextField } from 'react-admin';

export const AuditList = () => (
    <List>
        <Datagrid rowClick="edit">
            <DateField source="time" showTime />
            <TextField source="type" />
            <TextField source="principal" />
            <TextField source="data.provider" />
        </Datagrid>
    </List>
);
