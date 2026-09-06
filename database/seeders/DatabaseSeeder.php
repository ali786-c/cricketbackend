<?php

namespace Database\Seeders;

use App\Models\User;
use Illuminate\Database\Console\Seeds\WithoutModelEvents;
use Illuminate\Database\Seeder;

class DatabaseSeeder extends Seeder
{
    public function run(): void
    {
        $this->call(RolePermissionSeeder::class);

        $admin = User::factory()->create([
            'name' => 'System Admin',
            'email' => 'admin@cricketdraft.test',
        ]);

        $admin->assignRole('admin');

        $superAdmin = User::factory()->create([
            'name' => 'Platform Super Admin',
            'email' => 'superadmin@cricketdraft.test',
        ]);
        $superAdmin->assignRole('super_admin');

        \App\Models\ApiClient::firstOrCreate(
            ['slug' => 'cricket-app-android'],
            [
                'name' => 'Android App',
                'platform' => 'android',
                'version' => '1.0.0',
                'rate_limit_per_minute' => 60,
                'is_active' => true,
                'created_by' => $superAdmin->id,
            ]
        );
    }
}
